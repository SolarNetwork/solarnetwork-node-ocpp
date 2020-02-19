/* ==================================================================
 * BaseEntityManager.java - 14/02/2020 7:15:19 am
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Abstract class to help with exposing a DAO as a list of settings that can be
 * managed.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseEntityManager<D extends GenericDao<T, K>, T extends Entity<K> & Differentiable<T>, K, C extends Identity<K>>
		implements SettingSpecifierProvider, SettingsChangeObserver {

	/** The DAO. */
	protected final D dao;
	private List<C> entities;
	private MessageSource messageSource;
	private int entitiesCount = -1;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	public BaseEntityManager(D dao) {
		super();
		this.dao = dao;
	}

	/**
	 * Create a new entity based on a a configuration.
	 * 
	 * @param conf
	 *        the configuration
	 * @return the new entity
	 */
	protected abstract T createNewEntity(C conf);

	/**
	 * Clone an entity.
	 * 
	 * @param entity
	 *        the entity to clone
	 * @return the cloned entity
	 */
	protected abstract T cloneEntity(T entity);

	/**
	 * Apply a configuration to an entity.
	 * 
	 * @param conf
	 *        the configuration
	 * @param entity
	 *        the entity
	 */
	protected abstract void applyConfiguration(C conf, T entity);

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}
		Map<K, T> all = dao.getAll(SORT_BY_CREATED_ASCENDING).stream()
				.collect(Collectors.toMap(e -> e.getId(), e -> e));
		List<C> configs = getEntities();
		Iterator<C> confItr = (configs != null ? configs.iterator() : Collections.emptyIterator());
		while ( confItr.hasNext() ) {
			C conf = confItr.next();
			if ( conf.getId() == null ) {
				continue;
			}
			T one = all.remove(conf.getId());
			if ( one == null ) {
				one = createNewEntity(conf);
			}
			T orig = cloneEntity(one);
			applyConfiguration(conf, one);
			if ( one.differsFrom(orig) ) {
				log.info("Saving updated entity: {}", one);
				dao.save(one);
			}
		}
		for ( T old : all.values() ) {
			log.info("Deleting entity: {}", old);
			dao.delete(old);
		}
	}

	/**
	 * Generate a list of settings for a single entity configuration.
	 * 
	 * @param entity
	 *        the entity configuration
	 * @param index
	 *        the configuration index
	 * @param keyPrefix
	 *        a setting key prefix to use
	 * @return the list of settings, never {@literal null}
	 */
	protected abstract List<SettingSpecifier> settingsForConfiguration(C entity, int index,
			String keyPrefix);

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(1);

		List<C> configs = getEntities();
		results.add(SettingsUtil.dynamicListSettingSpecifier("entities", configs,
				new SettingsUtil.KeyedListCallback<C>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(C value, int index,
							String key) {
						return Collections.singletonList(new BasicGroupSettingSpecifier(
								settingsForConfiguration(value, index, key + ".")));
					}
				}));

		return results;
	}

	/**
	 * Get a function that maps from entities to entity entities.
	 * 
	 * @return the function, e.g. {@code C::new}
	 */
	protected abstract Function<? super T, ? extends C> mapToConfiguration();

	private List<C> loadEntities() {
		Collection<T> result = dao.getAll(SORT_BY_CREATED_ASCENDING);
		List<C> configs = (result != null
				? result.stream().map(mapToConfiguration()).collect(Collectors.toList())
				: new ArrayList<>());
		if ( this.entities != null ) {
			while ( configs.size() < this.entitiesCount ) {
				configs.add(createNewConfiguration());
			}
		} else {
			this.entitiesCount = configs.size();
		}
		this.entities = configs;
		return configs;
	}

	/**
	 * Get the message source.
	 * 
	 * @return the message source
	 */
	@Override
	public MessageSource getMessageSource() {
		return messageSource;
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
	 * Get the list of entity entities.
	 * 
	 * @return the entities
	 */
	public synchronized List<C> getEntities() {
		if ( entities == null ) {
			loadEntities();
		}
		return entities;
	}

	/**
	 * Set the list of entity entities.
	 * 
	 * @param entities
	 *        the entities to set
	 */
	public synchronized void setEntities(List<C> entities) {
		this.entities = entities;
	}

	/**
	 * Get the count of entity entities.
	 * 
	 * @return the configuration count
	 */
	public synchronized int getEntitiesCount() {
		if ( entitiesCount < 0 ) {
			loadEntities();
		}
		return entitiesCount;
	}

	/**
	 * Create a new entity configuration instance.
	 * 
	 * @return the new instance
	 */
	protected abstract C createNewConfiguration();

	/**
	 * Adjust the number of configured entity entities.
	 * 
	 * <p>
	 * Any newly added element values will be set to
	 * {@link #createNewConfiguration()} instances.
	 * </p>
	 * 
	 * @param count
	 *        the desired number of elements
	 */
	public void setEntitiesCount(int count) {
		List<C> confs = getEntities();
		this.entitiesCount = count;

		int currCount = (confs != null ? confs.size() : 0);
		if ( currCount == count ) {
			return;
		}
		while ( currCount < count ) {
			confs.add(createNewConfiguration());
			currCount++;
		}
		while ( currCount > count ) {
			confs.remove(--currCount);
		}
	}

}
