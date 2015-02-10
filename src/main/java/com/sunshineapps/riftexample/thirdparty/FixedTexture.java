package com.sunshineapps.riftexample.thirdparty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.media.opengl.GL2;

public final class FixedTexture {
    private final int[] id;
    private final ByteBuffer buffer;
    
    public FixedTexture(GL2 gl3, int width, int height, byte[] data) {
        int[] textureId = new int[1];
        gl3.glGenTextures(1, textureId, 0);
        gl3.glBindTexture(GL2.GL_TEXTURE_2D, textureId[0]);
        
        buffer = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
        buffer.put(data, 0, data.length);
        buffer.rewind();

        gl3.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, width, height, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, buffer);
        gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 0);
        id = textureId;
    }

    public FixedTexture(GL2 gl3, BuiltinTexture builtinTexture) {
        this(gl3, 256, 256, generateTexture(builtinTexture));
    }
    
    private static byte[] generateTexture(BuiltinTexture builtinTexture) {
        byte[] data = new byte[256 * 256 * 4];

        switch (builtinTexture) {
            case tex_checker:
                for (int j = 0; j < 256; j++) {
                    for (int i = 0; i < 256; i++) {
                        boolean colorB = (((i / 4 >> 5) ^ (j / 4 >> 5)) & 1) == 1;
                        data[(j * 256 + i) * 4 + 0] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 1] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 2] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 3] = (byte)(colorB ? 255 : 255);
                    }
                }
                break;

            case tex_panel:
                for (int j = 0; j < 256; j++) {
                    for (int i = 0; i < 256; i++) {
                        boolean colorB = (i / 4 == 0 || j / 4 == 0);
                        data[(j * 256 + i) * 4 + 0] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 1] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 2] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 3] = (byte)(colorB ? 255 : 255);
                    }
                }
                break;

            default:
                for (int j = 0; j < 256; j++) {
                    for (int i = 0; i < 256; i++) {
                        boolean colorB = ((j / 4 & 15) == 0) || (((i / 4 & 15) == 0) && ((((i / 4 & 31) == 0) ^ (((j / 4 >> 4) & 1)== 1)) == false));
                        data[(j * 256 + i) * 4 + 0] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 1] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 2] = (byte)(colorB ? 80 : 180);
                        data[(j * 256 + i) * 4 + 3] = (byte)(colorB ? 255 : 255);
                    }
                }
        }
        return data;
    }

    public int getId() {
        return id[0];
    }

    public enum BuiltinTexture {
        tex_none,
        tex_checker,
        tex_panel,
        tex_count
    }
}