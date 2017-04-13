package com.etesync.syncadapter.model;

import java.util.LinkedList;
import java.util.List;

import io.requery.Column;
import io.requery.Convert;
import io.requery.Converter;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Index;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Persistable;
import io.requery.PostLoad;
import io.requery.ReferentialAction;
import io.requery.Table;
import io.requery.sql.EntityDataStore;

public class JournalModel {
    // FIXME: Add unique constraint on the uid + service combination. Can't do it at the moment because requery is broken.
    @Entity
    @Table(name = "Journal")
    public static abstract class Journal {
        @Key
        @Generated
        int id;

        @Column(length = 64, nullable = false)
        String uid;

        @Convert(CollectionInfoConverter.class)
        CollectionInfo info;

        String owner;

        byte[] encryptedKey;


        @Deprecated
        long service;

        @ForeignKey(update = ReferentialAction.CASCADE)
        @ManyToOne
        Service serviceModel;

        boolean deleted;

        @PostLoad
        void afterLoad() {
            this.info.serviceID = this.serviceModel.id;
            this.info.uid = uid;
        }

        public Journal() {
            this.deleted = false;
        }

        public Journal(EntityDataStore<Persistable> data, CollectionInfo info) {
            this();
            this.info = info;
            this.uid = info.uid;
            this.serviceModel = info.getServiceEntity(data);
        }

        public static List<JournalEntity> getJournals(EntityDataStore<Persistable> data, ServiceEntity serviceEntity) {
            return data.select(JournalEntity.class).where(JournalEntity.SERVICE_MODEL.eq(serviceEntity).and(JournalEntity.DELETED.eq(false))).get().toList();
        }

        public static List<CollectionInfo> getCollections(EntityDataStore<Persistable> data, ServiceEntity serviceEntity) {
            List<CollectionInfo> ret = new LinkedList<>();

            List<JournalEntity> journals = getJournals(data, serviceEntity);
            for (JournalEntity journal : journals) {
                // FIXME: For some reason this isn't always being called, manually do it here.
                journal.afterLoad();
                ret.add(journal.getInfo());
            }

            return ret;
        }

        public static JournalEntity fetch(EntityDataStore<Persistable> data, ServiceEntity serviceEntity, String uid) {
            JournalEntity ret = data.select(JournalEntity.class).where(JournalEntity.SERVICE_MODEL.eq(serviceEntity).and(JournalEntity.UID.eq(uid))).limit(1).get().firstOrNull();
            if (ret != null) {
                // FIXME: For some reason this isn't always being called, manually do it here.
                ret.afterLoad();
            }
            return ret;
        }

        public static JournalEntity fetchOrCreate(EntityDataStore<Persistable> data, CollectionInfo collection) {
            JournalEntity journalEntity = fetch(data, collection.getServiceEntity(data), collection.uid);
            if (journalEntity == null) {
                journalEntity = new JournalEntity(data, collection);
            } else {
                journalEntity.setInfo(collection);
            }
            return journalEntity;
        }

        public String getLastUid(EntityDataStore<Persistable> data) {
            EntryEntity last = data.select(EntryEntity.class).where(EntryEntity.JOURNAL.eq(this)).orderBy(EntryEntity.ID.desc()).limit(1).get().firstOrNull();
            if (last != null) {
                return last.getUid();
            }

            return null;
        }
    }

    @Entity
    @Table(name = "Entry", uniqueIndexes = "entry_unique_together")
    public static abstract class Entry {
        @Key
        @Generated
        int id;

        @Index("entry_unique_together")
        @Column(length = 64, nullable = false)
        String uid;

        @Convert(SyncEntryConverter.class)
        SyncEntry content;

        @Index("entry_unique_together")
        @ForeignKey(update = ReferentialAction.CASCADE)
        @ManyToOne
        Journal journal;
    }


    @Entity
    @Table(name = "Service", uniqueIndexes = "service_unique_together")
    public static abstract class Service {
        @Key
        @Generated
        int id;

        @Index(value = "service_unique_together")
        @Column(nullable = false)
        String account;

        @Index(value = "service_unique_together")
        CollectionInfo.Type type;

        public static ServiceEntity fetch(EntityDataStore<Persistable> data, String account, CollectionInfo.Type type) {
            return data.select(ServiceEntity.class).where(ServiceEntity.ACCOUNT.eq(account).and(ServiceEntity.TYPE.eq(type))).limit(1).get().firstOrNull();
        }
    }

    static class CollectionInfoConverter implements Converter<CollectionInfo, String> {
        @Override
        public Class<CollectionInfo> getMappedType() {
            return CollectionInfo.class;
        }

        @Override
        public Class<String> getPersistedType() {
            return String.class;
        }

        @Override
        public Integer getPersistedSize() {
            return null;
        }

        @Override
        public String convertToPersisted(CollectionInfo value) {
            return value == null ? null : value.toJson();
        }

        @Override
        public CollectionInfo convertToMapped(Class<? extends CollectionInfo> type, String value) {
            return value == null ? null : CollectionInfo.fromJson(value);
        }
    }


    static class SyncEntryConverter implements Converter<SyncEntry, String> {
        @Override
        public Class<SyncEntry> getMappedType() {
            return SyncEntry.class;
        }

        @Override
        public Class<String> getPersistedType() {
            return String.class;
        }

        @Override
        public Integer getPersistedSize() {
            return null;
        }

        @Override
        public String convertToPersisted(SyncEntry value) {
            return value == null ? null : value.toJson();
        }

        @Override
        public SyncEntry convertToMapped(Class<? extends SyncEntry> type, String value) {
            return value == null ? null : SyncEntry.fromJson(value);
        }
    }
}
