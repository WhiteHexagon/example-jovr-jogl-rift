package com.sunshineapps.riftexample.thirdparty;
import com.oculusvr.capi.OvrSizei;
import javax.media.opengl.GL2;

/**
 *
 * @author gbarbieri
 */
public class FrameBuffer {

    private int[] id;
    private OvrSizei size;
    private int[] textureId;
    private int[] depthId;

    public FrameBuffer(GL2 gl3, OvrSizei size) {

        this.size = size;

        id = new int[1];
        gl3.glGenFramebuffers(1, id, 0);
        gl3.glBindFramebuffer(GL2.GL_FRAMEBUFFER, id[0]);
        {
            textureId = new int[1];
            gl3.glGenTextures(1, textureId, 0);

            gl3.glBindTexture(GL2.GL_TEXTURE_2D, textureId[0]);
            {
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);

                gl3.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, size.w, size.h, 0, GL2.GL_RGBA, GL2.GL_FLOAT, null);

                gl3.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, textureId[0], 0);
            }
            depthId = new int[1];
            gl3.glGenTextures(1, depthId, 0);

            gl3.glBindTexture(GL2.GL_TEXTURE_2D, depthId[0]);
            {
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
                gl3.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);

                gl3.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT32F, size.w, size.h, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, null);

                gl3.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, textureId[0], 0);
            }
            gl3.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_TEXTURE_2D, depthId[0], 0);

            if (gl3.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE) {

                System.out.println("FrameBuffer incomplete!");
            }
        }
        gl3.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

    }

    public int getTextureId() {
        return textureId[0];
    }

    public int getId() {
        return id[0];
    }

    public OvrSizei getSize() {
        return size;
    }
}
