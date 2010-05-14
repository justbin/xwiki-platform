/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.internal.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Specialized cache component related to documents. It automatically clean the cache when the document is related.
 * 
 * @param <C> the type of the data stored in the cache
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultDocumentCache<C> implements DocumentCache<C>
{
    /**
     * Event listened by the component.
     */
    private static final List<Event> EVENTS =
        Arrays.<Event> asList(new DocumentSaveEvent(), new DocumentUpdateEvent(), new DocumentDeleteEvent());

    /**
     * Used to initialize the actual cache component.
     */
    @Requirement
    private CacheManager cacheManager;

    /**
     * Serialize a document reference into a String.
     */
    @Requirement
    private EntityReferenceSerializer<String> serializer;

    /**
     * The actual cache object.
     */
    private Cache<C> cache;

    /**
     * The cache used to follow multiple cache entries related to the same document.
     */
    private Cache<Collection<String>> mappingCache;

    /**
     * The identifier of the cache and event listener.
     */
    private String name;

    /**
     * Used to register as event listener to invalidate the cache.
     */
    @Requirement
    private ObservationManager observationManager;

    /**
     * Used to listen to document modification events.
     * 
     * @version $Id$
     */
    private class Listener implements EventListener
    {
        /**
         * {@inheritDoc}
         * 
         * @see org.xwiki.observation.EventListener#getName()
         */
        public String getName()
        {
            return name;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xwiki.observation.EventListener#getEvents()
         */
        public List<Event> getEvents()
        {
            return EVENTS;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
         *      java.lang.Object)
         */
        public void onEvent(Event event, Object source, Object data)
        {
            XWikiDocument document = (XWikiDocument) source;

            String documentReferenceString = serializer.serialize(document.getDocumentReference());

            Collection<String> keys = mappingCache.get(documentReferenceString);

            if (keys != null) {
                for (String key : keys) {
                    cache.remove(key);
                }

                mappingCache.remove(documentReferenceString);
            }
        }
    }

    /**
     * The listener used to listen to document modification events.
     */
    private Listener listener = new Listener();

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.DocumentCache#create(org.xwiki.cache.config.CacheConfiguration)
     */
    public void create(CacheConfiguration cacheConfiguration) throws CacheException
    {
        this.name = cacheConfiguration.getConfigurationId();

        this.cache = this.cacheManager.createNewCache(cacheConfiguration);

        CacheConfiguration mappingCacheConfiguration = (CacheConfiguration) cacheConfiguration.clone();
        mappingCacheConfiguration.setConfigurationId(cacheConfiguration.getConfigurationId() + ".mapping");

        this.mappingCache = this.cacheManager.createNewCache(cacheConfiguration);

        this.observationManager.addListener(this.listener);
    }

    // cache

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.DocumentCache#get(org.xwiki.model.reference.DocumentReference,
     *      java.lang.Object[])
     */
    public C get(DocumentReference documentReference, Object... extensions)
    {
        return this.cache.get(getKey(documentReference, extensions));
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.DocumentCache#set(java.lang.Object,
     *      org.xwiki.model.reference.DocumentReference, java.lang.Object[])
     */
    public void set(C data, DocumentReference documentReference, Object... extensions)
    {
        String key = getKey(documentReference, extensions);
        this.cache.set(key, data);

        String documentReferenceString = this.serializer.serialize(documentReference);

        Collection<String> keys = this.mappingCache.get(documentReferenceString);

        if (keys == null) {
            keys = new HashSet<String>();
            this.mappingCache.set(documentReferenceString, keys);
        }

        keys.add(key);
    }

    /**
     * Generate a key based on the provided document reference and extensions.
     * 
     * @param documentReference the reference of the document
     * @param extensions the extensions to the document reference
     * @return the value
     */
    protected String getKey(DocumentReference documentReference, Object... extensions)
    {
        StringBuffer buffer = new StringBuffer();

        if (extensions.length > 0) {
            buffer.append(escape(this.serializer.serialize(documentReference)));
            for (Object extension : extensions) {
                buffer.append(':');
                buffer.append(escape(extension != null ? extension.toString() : ""));
            }
        } else {
            buffer.append(this.serializer.serialize(documentReference));
        }

        return buffer.toString();
    }

    /**
     * Escape each element of the key.
     * 
     * @param str the element of the key to escape
     * @return the escaped key element
     */
    private String escape(String str)
    {
        return str.replace("\\", "\\\\").replace(":", "\\:");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.DocumentCache#remove(java.lang.Object,
     *      org.xwiki.model.reference.DocumentReference, java.lang.Object[])
     */
    public void remove(C data, DocumentReference documentReference, Object... extensions)
    {
        String key = getKey(documentReference, extensions);
        this.cache.remove(key);

        String documentReferenceString = serializer.serialize(documentReference);

        Collection<String> keys = mappingCache.get(documentReferenceString);

        if (keys != null) {
            keys.remove(key);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.DocumentCache#removeAll()
     */
    public void removeAll()
    {
        this.cache.removeAll();
        this.mappingCache.removeAll();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.DocumentCache#dispose()
     */
    public void dispose()
    {
        this.cache.dispose();
        this.mappingCache.dispose();
    }
}
