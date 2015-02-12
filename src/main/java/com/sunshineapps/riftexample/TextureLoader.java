package com.sunshineapps.riftexample;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public final class TextureLoader {

    public Texture getTexture(GL2 gl, String resourceName) throws IOException {
        URL url = TextureLoader.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IOException("Cannot find: "+resourceName);
        }
        
        BufferedImage bufferedImage = ImageIO.read(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(resourceName))); 
      //  ImageUtil.flipImageVertically(bufferedImage);
        Texture result = AWTTextureIO.newTexture(GLProfile.getDefault(), bufferedImage, true);
        result.enable(gl);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        return result;
    }

}