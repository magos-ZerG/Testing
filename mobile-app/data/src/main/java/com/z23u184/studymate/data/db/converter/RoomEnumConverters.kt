package com.z23u184.studymate.data.db.converter

import androidx.room.TypeConverter
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.db.entity.SyncQueueStatus
import com.z23u184.studymate.data.db.entity.SyncSessionStatus
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.domain.model.AttachmentUploadState

class RoomEnumConverters {
    @TypeConverter fun fromEntitySyncState(value: EntitySyncState): String = value.name
    @TypeConverter fun toEntitySyncState(value: String): EntitySyncState = EntitySyncState.valueOf(value)

    @TypeConverter fun fromOwnerType(value: OwnerType): String = value.name
    @TypeConverter fun toOwnerType(value: String): OwnerType = OwnerType.valueOf(value)

    @TypeConverter fun fromSyncEntityType(value: SyncEntityType): String = value.name
    @TypeConverter fun toSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

    @TypeConverter fun fromSyncOperation(value: SyncOperation): String = value.name
    @TypeConverter fun toSyncOperation(value: String): SyncOperation = SyncOperation.valueOf(value)

    @TypeConverter fun fromQueueStatus(value: SyncQueueStatus): String = value.name
    @TypeConverter fun toQueueStatus(value: String): SyncQueueStatus = SyncQueueStatus.valueOf(value)

    @TypeConverter fun fromSessionStatus(value: SyncSessionStatus): String = value.name
    @TypeConverter fun toSessionStatus(value: String): SyncSessionStatus = SyncSessionStatus.valueOf(value)

    @TypeConverter fun fromTrigger(value: SyncTrigger): String = value.name
    @TypeConverter fun toTrigger(value: String): SyncTrigger = SyncTrigger.valueOf(value)

    @TypeConverter fun fromUploadState(value: AttachmentUploadState): String = value.name
    @TypeConverter fun toUploadState(value: String): AttachmentUploadState = AttachmentUploadState.valueOf(value)
}
