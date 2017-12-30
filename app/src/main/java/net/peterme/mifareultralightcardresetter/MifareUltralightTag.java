package net.peterme.mifareultralightcardresetter;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareUltralight;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

/**
 * Represents a MifareUltralight NFC tag.
 */
public class MifareUltralightTag {
    private static final byte[] NULL_PAGE = new byte[MifareUltralight.PAGE_SIZE];
    private static final byte[] NULL_BLOCK = new byte[4 * MifareUltralight.PAGE_SIZE];
    private static final byte GET_VERSION_CMD = 0x60;
    private static final byte[] GET_VERSION_BYTES = new byte[] { GET_VERSION_CMD };
    public static final String LOG_TAG = "MifareUltralight";

    private final @NonNull String name;
    private final int tagType;
    private final @NonNull byte[][] pages;

    private MifareUltralightTag(@NonNull String name, int tagType, @NonNull byte[][] pages) {
        this.name = name;
        this.tagType = tagType;
        this.pages = pages;
    }

    public @NonNull String getName() {
        return this.name;
    }

    public int getTagType() {
        return this.tagType;
    }

    private byte[] getPageBytesOrNull(int index) {
        if (index < 0 || index >= this.pages.length) {
            return null;
        }
        return this.pages[index];
    }

    private static byte getByteFromArray(@Nullable byte[] bytes, int index) {
        return getByteFromArray(bytes, index, (byte)0);
    }

    private static byte getByteFromArray(@Nullable byte[] bytes, int index, byte defaultValue) {
        if (bytes == null || index < 0 || index >= bytes.length) {
            return defaultValue;
        }
        return bytes[index];
    }

    private void checkPageIndex(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= this.pages.length) {
            throw new IndexOutOfBoundsException(String.format(Locale.ENGLISH,
                    "pageCount: %d, index: %d",
                    this.pages.length,
                    index
            ));
        }
    }

    public long getUidSerialNumber() {
        byte[] uidBytes0 = this.pages[0];
        byte[] uidBytes1 = this.pages[1];
        long result = 0;
        result |= (long)getByteFromArray(uidBytes0, 0) << (6 * Byte.SIZE);
        result |= (long)getByteFromArray(uidBytes0, 1) << (5 * Byte.SIZE);
        result |= (long)getByteFromArray(uidBytes0, 2) << (4 * Byte.SIZE);
        result |= (long)getByteFromArray(uidBytes1, 0) << (3 * Byte.SIZE);
        result |= (long)getByteFromArray(uidBytes1, 1) << (2 * Byte.SIZE);
        result |= (long)getByteFromArray(uidBytes1, 2) << (Byte.SIZE);
        result |= (long)getByteFromArray(uidBytes1, 3);
        return result;
    }

    public byte getUidCheck0() {
        byte[] page0Bytes = this.pages[0];
        return getByteFromArray(page0Bytes, 3);
    }

    public byte getUidCheck1() {
        byte[] page2Bytes = this.pages[2];
        return getByteFromArray(page2Bytes, 0);
    }

    public boolean isPageWritable(int index) throws IndexOutOfBoundsException {
        if (index < 0) {
            throw new IndexOutOfBoundsException(String.format(Locale.ENGLISH,
                    "Index is negative. pageCount: %d, index: %d",
                    this.pages.length,
                    index
            ));
        }
        // All MIFARE Ultralight cards have the same layout for pages 0-15
        // with two lock bytes in page 2
        switch (index) {
            case 0:
            case 1:
                return false; // Always UID Pages
            case 2:
                return false; // Partially writable
        }
        if (index < 16) {
            byte[] page2Bytes = getPageBytesOrNull(2);
            if (index < 8) { // Page 3-7
                byte lockByte2 = getByteFromArray(page2Bytes, 2, (byte)-1);
                return (lockByte2 & (1 << index)) == 0;
            } else { // Page 8-15
                byte lockByte3 = getByteFromArray(page2Bytes, 3, (byte)-1);
                return (lockByte3 & (1 << (index - 8))) == 0;
            }
        } else if (this.tagType == MifareUltralight.TYPE_ULTRALIGHT && index < 36) {
            // MIFARE Ultralight EV1 (has pages 16-40)
            // two more lock bytes in page 0x24 (36)
            // with page-granularity of 2
            byte[] page24hBytes = getPageBytesOrNull(0x24);
            if (index < 32) { // Page 16-31
                byte lockByte0 = getByteFromArray(page24hBytes, 0, (byte)-1);
                int bit = index / 2 - 8; // 2-page granularity starting at page 16
                return (lockByte0 & (1 << bit)) == 0;
            } else { // Page 32-35
                byte lockByte1 = getByteFromArray(page24hBytes, 1, (byte)-1);
                int bit = index / 2 - 16; // 2-page granularity starting at page 32
                return (lockByte1 & (1 << bit)) == 0;
            }
        }
        return false;
    }

    public boolean isAccessModifiable(int index) throws IndexOutOfBoundsException {
        if (index < 0) {
            throw new IndexOutOfBoundsException(String.format(Locale.ENGLISH,
                    "Index is negative. pageCount: %d, index: %d",
                    this.pages.length,
                    index
            ));
        }
        // All MIFARE Ultralight cards have the same layout for pages 0-15
        // with block locks in page 2
        switch (index) {
            case 0:
            case 1:
            case 2:
                return false;
        }
        if (index < 16) { // Pages 3-15
            byte[] page2Bytes = getPageBytesOrNull(2);
            byte lockByte2 = getByteFromArray(page2Bytes, 2, (byte)-1);
            final int bit;
            if (index == 3) { // OTP-Page
                bit = 0;
            } else if (index < 10) { // Pages 4-9
                bit = 1;
            } else { // Pages 10-15
                bit = 2;
            }
            return (lockByte2 & (1 << bit)) == 0;
        } else if (this.tagType == MifareUltralight.TYPE_ULTRALIGHT && index < 36) {
            // MIFARE Ultralight EV1 (has pages 16-40)
            // block locks in page 0x24 (36)
            byte[] page24hBytes = getPageBytesOrNull(0x24);
            byte lockByte2 = getByteFromArray(page24hBytes, 2, (byte)-1);
            final int bit = index / 4 - 4;
            return (lockByte2 & (1 << bit)) == 0;
        }
        return false;
    }

    public @NonNull byte[] getPageCopy(int index) throws IndexOutOfBoundsException {
        checkPageIndex(index);
        byte[] copy = new byte[MifareUltralight.PAGE_SIZE];
        byte[] src = this.pages[index];
        if (src == null || src.length < MifareUltralight.PAGE_SIZE) {
            src = NULL_PAGE;
        }
        System.arraycopy(
                src, 0,
                copy, 0,
                MifareUltralight.PAGE_SIZE
        );
        return copy;
    }

    public void getPage(int index, @NonNull byte[] dst)
            throws IndexOutOfBoundsException {
        getPage(index, dst, 0);
    }

    public void getPage(int index, @NonNull byte[] dst, int offset)
        throws IndexOutOfBoundsException {
        checkPageIndex(index);
        if (offset < 0 || (offset + MifareUltralight.PAGE_SIZE) >= dst.length) {
            throw new ArrayIndexOutOfBoundsException(offset);
        }
        byte[] src = this.pages[index];
        if (src == null || src.length < MifareUltralight.PAGE_SIZE) {
            src = NULL_PAGE;
        }
        System.arraycopy(
                src, 0,
                dst, offset,
                MifareUltralight.PAGE_SIZE
        );
    }

    public int getPageCount() {
        return this.pages.length;
    }

    public static int getPageCount(@NonNull MifareUltralight tag) throws IOException {
        final int tagType = tag.getType();
        switch (tagType) {
            case MifareUltralight.TYPE_ULTRALIGHT: {
                int getVersionPageCount;
                if (!tag.isConnected()) {
                    try {
                        tag.connect();
                        getVersionPageCount = getPageCountGetVersion(tag);
                    } finally {
                        try {
                            tag.close();
                        } catch (IOException ioExcept) {
                            Log.wtf(LOG_TAG, "I/O Exception while closing connection.", ioExcept);
                        }
                    }
                } else {
                    getVersionPageCount = getPageCountGetVersion(tag);
                }
                if (getVersionPageCount < 0) {
                    return (0x0F) + 1;
                }
                return getVersionPageCount;
            }
            case MifareUltralight.TYPE_ULTRALIGHT_C:
                return (0x2B) + 1;
            default:
                return 0;
        }
    }

    private static int getPageCountGetVersion(@NonNull MifareUltralight mulTag) throws IOException {
        byte[] getVersionBytes = mulTag.transceive(GET_VERSION_BYTES);
        if (getVersionBytes == null || getVersionBytes.length == 1) {
            Log.i(LOG_TAG, getVersionBytes == null ? "null" : "NACK" +
                    " response for GET_VERSION command: " +
                    Byte.toString(getByteFromArray(getVersionBytes, 0))
            );
            return -1;
        }
        byte vendorId = getByteFromArray(getVersionBytes, 1);
        byte productType = getByteFromArray(getVersionBytes, 2);
        byte productSubType = getByteFromArray(getVersionBytes, 3);
        byte majorVersion = getByteFromArray(getVersionBytes, 4);
        byte minorVersion = getByteFromArray(getVersionBytes, 5);
        byte storageSize = getByteFromArray(getVersionBytes, 6);
        int exp = ((int)storageSize) >> 1;
        int minUserBytes = 1 << exp; // 2^exp
        int maxUserBytes = minUserBytes;
        if ((storageSize & 0x1) != 0) {
            maxUserBytes <<= 1; // 2^(exp + 1)
        }
        int minUserPages = minUserBytes / MifareUltralight.PAGE_SIZE;
        int maxUserPages = maxUserBytes / MifareUltralight.PAGE_SIZE;
        int pageCount = 0;
        if (minUserBytes <= 32) {
            pageCount = 8; // 8 non-user pages
            pageCount += maxUserPages;
        } else if (minUserPages <= 32) {
            pageCount = 9; // 9 non-user pages
            pageCount += maxUserPages;
        }
        byte protocolType = getByteFromArray(getVersionBytes, 7);
        StringBuilder msg = new StringBuilder()
                .append("GET_VERSION")
                .append(System.lineSeparator())
                .append("\tVendor ID: 0x").append(Integer.toHexString(vendorId))
                .append(vendorId == 0x4 ? " (NXP Semiconductors)" : "")
                .append(System.lineSeparator())
                .append("\tProduct type: 0x").append(Integer.toHexString(productType))
                .append(productType == 0x3 ? " (MIFARE Ultralight)" : "")
                .append(System.lineSeparator())
                .append("\tProduct subtype: 0x").append(Integer.toHexString(productSubType))
                .append(productSubType == 0x1 ? " (17 pF)" : (productSubType == 0x2 ? " (50 pF)" : ""))
                .append(System.lineSeparator())
                .append("\tProduct version: ").append(majorVersion).append('.').append(minorVersion)
                .append(majorVersion == 0x1 ? " (EV1" + (minorVersion == 0 ? " V0" : "") + ")" : "")
                .append(System.lineSeparator())
                .append("\tUser memory size: ").append(minUserBytes)
                .append(maxUserBytes > minUserBytes ? " - " + Integer.toString(maxUserBytes) : "")
                .append(" bytes")
                .append(System.lineSeparator())
                .append("\tUser memory pages: ").append(minUserPages)
                .append(maxUserPages > minUserPages ? " - " + Integer.toString(maxUserPages) : "")
                .append(System.lineSeparator())
                .append("\tPage count: ").append(pageCount)
                .append(System.lineSeparator())
                .append("\tProtocol type: 0x").append(Integer.toHexString(protocolType))
                .append(protocolType == 0x3 ? " (ISO/IEC 14443-3 compliant)" : "")
                ;
        Log.v(LOG_TAG, msg.toString());
        return pageCount;
    }

    public static @Nullable MifareUltralightTag fromNfc(
            @NonNull String name,
            @NonNull Tag nfcTag) throws IOException {
        MifareUltralight mulTag = MifareUltralight.get(nfcTag);
        if (mulTag == null) {
            Log.w(LOG_TAG, String.format(Locale.ENGLISH,
                    "NFC Tag or Android System do not support MIFARE Ultralight. Tag: %s",
                    nfcTag.toString()
            ));
            return null;
        }
        return fromNfc(name, mulTag);
    }

    public static @NonNull MifareUltralightTag fromNfc(
            @NonNull String name,
            @NonNull MifareUltralight mulTag) throws IOException {
        byte[][] pages;
        final int tagType = mulTag.getType();
        try {
            mulTag.connect();
            final int pageCount = getPageCount(mulTag);
            pages = new byte[pageCount][];
            for (int pageOffset = 0; pageOffset < pageCount; pageOffset += 4) {
                byte[] pageBlock = mulTag.readPages(pageOffset);
                if (pageBlock == null) {
                    pageBlock = NULL_BLOCK;
                }
                for (int i = 0, pageIdx = pageOffset;
                        i < pageBlock.length;
                        i += MifareUltralight.PAGE_SIZE, pageIdx++) {
                    pages[pageIdx] = new byte[MifareUltralight.PAGE_SIZE];
                    System.arraycopy(
                            pageBlock, i,
                            pages[pageIdx], 0,
                            MifareUltralight.PAGE_SIZE
                    );
                }
            }
        } catch (TagLostException tagLostExcept) {
            Log.w(LOG_TAG, tagLostExcept);
            throw tagLostExcept;
        } catch (IOException ioExcept) {
            Log.e(LOG_TAG, "I/O Exception during read operation", ioExcept);
            throw ioExcept;
        } finally {
            try {
                mulTag.close();
            } catch (IOException ioExcept) {
                Log.wtf(LOG_TAG, "I/O Exception while closing connection.", ioExcept);
            }
        }
        return new MifareUltralightTag(name, tagType, pages);
    }
}
