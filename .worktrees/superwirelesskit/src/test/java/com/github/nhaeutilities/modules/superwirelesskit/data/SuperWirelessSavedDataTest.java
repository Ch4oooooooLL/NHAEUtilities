package com.github.nhaeutilities.modules.superwirelesskit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

public class SuperWirelessSavedDataTest {

    @Test
    public void roundTripsBindingRecordsWithBinderIdentityAndFingerprints() {
        SuperWirelessSavedData savedData = new SuperWirelessSavedData("test");
        BindingRecord expected = new BindingRecord(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            new ControllerEndpointRef(0, 10, 64, 10, ForgeDirection.NORTH, "appeng.tile.networking.TileController"),
            new BindingTargetRef(
                BindingTargetKind.PART,
                0,
                20,
                70,
                20,
                ForgeDirection.EAST,
                new BindingFingerprint("appliedenergistics2:tile.BlockCableBus", "appeng.parts.misc.PartInterface")),
            1234,
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            987654321L);

        savedData.put(expected);

        NBTTagCompound serialized = new NBTTagCompound();
        savedData.writeToNBT(serialized);

        SuperWirelessSavedData restored = new SuperWirelessSavedData("test");
        restored.readFromNBT(serialized);

        BindingRecord actual = restored.get(expected.getBindingId());
        assertNotNull(actual);
        assertEquals(expected, actual);
    }
}
