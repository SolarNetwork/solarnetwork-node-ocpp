/* ==================================================================
 * OcppAuthorizationManager.java - 11/02/2020 7:45:41 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.ocpp.v16.cs.controller;

import static net.solarnetwork.dao.GenericDao.SORT_BY_CREATED_ASCENDING;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.ocpp.dao.AuthorizationDao;
import net.solarnetwork.node.ocpp.domain.Authorization;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Manage {@link Authorization} entities via settings.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppAuthorizationManager implements SettingSpecifierProvider, SettingsChangeObserver {

	private final AuthorizationDao authorizationDao;
	private List<AuthorizationConfig> authorizations;
	private MessageSource messageSource;
	private int authorizationsCount = -1;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param authorizationDao
	 *        the DAO to manage authorizations with
	 */
	public OcppAuthorizationManager(AuthorizationDao authorizationDao) {
		super();
		this.authorizationDao = authorizationDao;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}
		Map<String, Authorization> auths = authorizationDao.getAll(SORT_BY_CREATED_ASCENDING).stream()
				.collect(Collectors.toMap(Authorization::getId, a -> a));
		List<AuthorizationConfig> configs = authorizations;
		Iterator<AuthorizationConfig> confItr = (configs != null ? configs.iterator()
				: Collections.emptyIterator());
		while ( confItr.hasNext() ) {
			AuthorizationConfig conf = confItr.next();
			if ( conf.getId() == null || conf.getId().isEmpty() ) {
				continue;
			}
			Authorization auth = auths.remove(conf.getId());
			if ( auth == null ) {
				auth = new Authorization(conf.getId(), Instant.now());
			}
			Authorization orig = new Authorization(auth);
			auth.setEnabled(conf.isEnabled());
			auth.setExpiryDate(conf.getExpiryDate());
			auth.setParentId(conf.getParentId());
			if ( !auth.isSameAs(orig) ) {
				log.info("Saving OCPP authorization: {}", auth);
				authorizationDao.save(auth);
			}
		}
		for ( Authorization old : auths.values() ) {
			authorizationDao.delete(old);
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.v16.cs.controller.OcppAuthorizationManager";
	}

	@Override
	public String getDisplayName() {
		return "OCPP Authorization Manager";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	private List<AuthorizationConfig> loadAuthorizations() {
		Collection<Authorization> result = authorizationDao.getAll(SORT_BY_CREATED_ASCENDING);
		List<AuthorizationConfig> configs = (result != null
				? result.stream().map(AuthorizationConfig::new).collect(Collectors.toList())
				: new ArrayList<>());
		if ( this.authorizations != null ) {
			while ( configs.size() < this.authorizationsCount ) {
				configs.add(new AuthorizationConfig());
			}
		} else {
			this.authorizationsCount = configs.size();
		}
		this.authorizations = configs;
		return configs;

	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(1);

		List<AuthorizationConfig> configs = getAuthorizations();
		results.add(SettingsUtil.dynamicListSettingSpecifier("authorizations", configs,
				new SettingsUtil.KeyedListCallback<AuthorizationConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(AuthorizationConfig value,
							int index, String key) {
						return Collections.singletonList(
								new BasicGroupSettingSpecifier(value.settings(key + ".")));
					}
				}));

		return results;
	}

	/**
	 * Set the message source.
	 * 
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the list of authorizations.
	 * 
	 * @return the authorizations
	 */
	public synchronized List<AuthorizationConfig> getAuthorizations() {
		if ( authorizations == null ) {
			loadAuthorizations();
		}
		return authorizations;
	}

	/**
	 * Set the list of authorizations.
	 * 
	 * @param authorizations
	 *        the authorizations to set
	 */
	public synchronized void setAuthorizations(List<AuthorizationConfig> authorizations) {
		this.authorizations = authorizations;
	}

	/**
	 * Get the count of authorizations.
	 * 
	 * @return the authorization count
	 */
	public synchronized int getAuthorizationsCount() {
		if ( authorizationsCount < 0 ) {
			loadAuthorizations();
		}
		return authorizationsCount;
	}

	/**
	 * Adjust the number of configured {@code AuthorizationConfig} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link AuthorizationConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        the desired number of elements
	 */
	public void setAuthorizationsCount(int count) {
		List<AuthorizationConfig> auths = getAuthorizations();
		this.authorizationsCount = count;

		int currCount = (auths != null ? auths.size() : 0);
		if ( currCount == count ) {
			return;
		}
		while ( currCount < count ) {
			auths.add(new AuthorizationConfig());
			currCount++;
		}
		while ( currCount > count ) {
			auths.remove(--currCount);
		}
	}

}
