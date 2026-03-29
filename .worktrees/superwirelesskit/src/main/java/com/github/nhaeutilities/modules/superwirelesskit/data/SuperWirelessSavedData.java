package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

public class SuperWirelessSavedData extends WorldSavedData {

    public static final String DATA_NAME = "nhaeutilities.superWirelessKit";
    private static final String RECORDS_KEY = "records";
    private static final String RECORD_KEY = "record";
    private static final int COMPOUND_TAG_ID = 10;

    private final Map<UUID, BindingRecord> records = new LinkedHashMap<UUID, BindingRecord>();

    public SuperWirelessSavedData() {
        this(DATA_NAME);
    }

    public SuperWirelessSavedData(String name) {
        super(name);
    }

    public void put(BindingRecord record) {
        records.put(record.getBindingId(), record);
        markDirty();
    }

    public BindingRecord get(UUID bindingId) {
        return records.get(bindingId);
    }

    public BindingRecord remove(UUID bindingId) {
        BindingRecord removed = records.remove(bindingId);
        if (removed != null) {
            markDirty();
        }
        return removed;
    }

    public Collection<BindingRecord> values() {
        return Collections.unmodifiableCollection(records.values());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        records.clear();
        NBTTagList list = tag.getTagList(RECORDS_KEY, COMPOUND_TAG_ID);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            BindingRecord record = BindingRecord.fromNbt(entry.getCompoundTag(RECORD_KEY));
            records.put(record.getBindingId(), record);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (BindingRecord record : records.values()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag(RECORD_KEY, record.toNbt());
            list.appendTag(entry);
        }
        tag.setTag(RECORDS_KEY, list);
    }
}
