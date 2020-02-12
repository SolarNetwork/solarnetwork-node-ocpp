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
		Map<String, Authorization> auths = authorizationDao.getAll(null).stream()
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
		Collection<Authorization> auths = authorizationDao.getAll(null);
		return (auths != null ? auths.stream().map(AuthorizationConfig::new).collect(Collectors.toList())
				: Collections.emptyList());
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(1);

		List<AuthorizationConfig> authConfsList = getAuthorizations();
		results.add(SettingsUtil.dynamicListSettingSpecifier("authorizations", authConfsList,
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
			authorizations = loadAuthorizations();
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
	public int getAuthorizationsCount() {
		List<AuthorizationConfig> auths = getAuthorizations();
		return (auths != null ? auths.size() : 0);
	}

	/**
	 * Adjust the number of configured {@code ExpressionConfig} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code expressionConfigs} elements.
	 * @since 1.5
	 */
	public void setAuthorizationsCount(int count) {
		List<AuthorizationConfig> auths = getAuthorizations();
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
