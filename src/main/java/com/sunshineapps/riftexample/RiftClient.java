package com.sunshineapps.riftexample;

import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_Chromatic;
import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_TimeWarp;
import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_Vignette;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Orientation;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Position;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.nativewindow.NativeWindowFactoryImpl;

import org.saintandreas.gl.MatrixStack;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Vector3f;

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.oculusvr.capi.EyeRenderDesc;
import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrLibrary.ovrEyeType;
import com.oculusvr.capi.OvrRecti;
import com.oculusvr.capi.OvrSizei;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.RenderAPIConfig;
import com.oculusvr.capi.Texture;
import com.oculusvr.capi.TextureHeader;
import com.sunshineapps.riftexample.thirdparty.FixedTexture;
import com.sunshineapps.riftexample.thirdparty.FixedTexture.BuiltinTexture;
import com.sunshineapps.riftexample.thirdparty.FrameBuffer;
import com.sunshineapps.riftexample.thirdparty.RiftUtils;

public class RiftClient implements KeyListener {
    private final AtomicBoolean shutdownRunning = new AtomicBoolean(false);
    
    //JOGL
    private Animator animator;
    private GLWindow glWindow;
   
    //Rift Specific
    private Hmd hmd;
    private int frameCount;
    private final OvrVector3f eyeOffsets[] = (OvrVector3f[])new OvrVector3f().toArray(2);
    private final OvrRecti[] eyeRenderViewport = (OvrRecti[]) new OvrRecti().toArray(2);
    private final Posef poses[] = (Posef[]) new Posef().toArray(2);
    private final Texture eyeTextures[] = (Texture[]) new Texture().toArray(2);
    private final FovPort fovPorts[] = (FovPort[]) new FovPort().toArray(2);
    private final Matrix4f projections[] = new Matrix4f[2];
    private final int fboIds[] = new int[2];
    private float ipd = OvrLibrary.OVR_DEFAULT_IPD;
    private float eyeHeight = OvrLibrary.OVR_DEFAULT_EYE_HEIGHT;
    
    //Scene
    private Matrix4f player;
     
    private final class DK2EventListener implements GLEventListener {
        private FrameBuffer leftEye;
        private FrameBuffer rightEye;
        private final FloatBuffer projectionDFB[] = new FloatBuffer[2];
        private final FloatBuffer modelviewDFB = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        private FixedTexture cheq;


        public void init(GLAutoDrawable drawable) {
            final GL2 gl = drawable.getGL().getGL2();
            gl.glClearColor(.42f, .67f, .87f, 1f);

            //Lighting
            float[] lightPos = new float[4];
            lightPos[0] =0.5f;
            lightPos[1] = 0;
            lightPos[2] = 1f;
            lightPos[3] = 0.0001f;
            gl.glEnable(GLLightingFunc.GL_LIGHTING);
            gl.glEnable(GLLightingFunc.GL_LIGHT0);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            float[] noAmbient = { 0.2f, 0.2f, 0.2f, 1f };
            float[] spec = { 1f, 1f, 1f, 1f };
            float[] diffuse = { 1f, 1f, 1f, 1f };
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, noAmbient, 0);
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, spec, 0);
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, diffuse, 0);
            gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPos, 0);
            gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 45.0f);

           
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
            
            RenderAPIConfig rc = new RenderAPIConfig();
            rc.Header.RTSize = hmd.Resolution;
            rc.Header.Multisample = 1;
            int distortionCaps = ovrDistortionCap_Chromatic | ovrDistortionCap_TimeWarp | ovrDistortionCap_Vignette;
            EyeRenderDesc eyeRenderDescs[] = hmd.configureRendering(rc, distortionCaps, fovPorts);
            for (int eye = 0; eye < 2; ++eye) {
            	eyeOffsets[eye].x = eyeRenderDescs[eye].HmdToEyeViewOffset.x;
            	eyeOffsets[eye].y = eyeRenderDescs[eye].HmdToEyeViewOffset.y;
            	eyeOffsets[eye].z = eyeRenderDescs[eye].HmdToEyeViewOffset.z;
            }
            
            leftEye = new FrameBuffer(gl, eyeRenderViewport[ovrEyeType.ovrEye_Left].Size);
            rightEye = new FrameBuffer(gl, eyeRenderViewport[ovrEyeType.ovrEye_Right].Size);
            fboIds[ovrEyeType.ovrEye_Left] = leftEye.getId();
            fboIds[ovrEyeType.ovrEye_Right] = rightEye.getId();
            
            eyeTextures[ovrEyeType.ovrEye_Left].TextureId = leftEye.getTextureId();
            eyeTextures[ovrEyeType.ovrEye_Right].TextureId = rightEye.getTextureId();

            //scene prep
            gl.glEnable(GL2.GL_TEXTURE_2D);
            cheq = FixedTexture.createBuiltinTexture(gl, BuiltinTexture.tex_checker);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        }

        public void dispose(GLAutoDrawable drawable) {
            // TODO Auto-generated method stub
        }

        public void display(GLAutoDrawable drawable) {
            hmd.beginFrameTiming(++frameCount);
            GL2 gl = drawable.getGL().getGL2();
            
            Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
            for (int eyeIndex = 0; eyeIndex < ovrEyeType.ovrEye_Count; eyeIndex++){
                int eye = hmd.EyeRenderOrder[eyeIndex];
                Posef pose = eyePoses[eye];
                poses[eye].Orientation = pose.Orientation;
                poses[eye].Position = pose.Position;

                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboIds[eye]);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadMatrixf(projectionDFB[eye]);
                
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                MatrixStack mv = MatrixStack.MODELVIEW;
                mv.push();
                {
                    mv.preTranslate(RiftUtils.toVector3f(poses[eye].Position).mult(-1));
                    mv.preRotate(RiftUtils.toQuaternion(poses[eye].Orientation).inverse());
                   // mv.preTranslate(RiftUtils.toVector3f(eyeRenderDescs[eye].ViewAdjust));
                    mv.translate(new Vector3f(0, eyeHeight, 0 ));
                    modelviewDFB.clear();
                    MatrixStack.MODELVIEW.top().fillFloatBuffer(modelviewDFB, true);
                    modelviewDFB.rewind();
                    gl.glLoadMatrixf(modelviewDFB);

                    //tiles on floor
                    gl.glEnable(GL2.GL_TEXTURE_2D);
                    gl.glBindTexture(GL2.GL_TEXTURE_2D, cheq.getId());
                    gl.glTranslatef(0.0f, -eyeHeight, 0.0f);
                    drawPlaneXZ(gl);
                    gl.glTranslatef(0.0f, eyeHeight, 0.0f);
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                mv.pop();
            }
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            
            hmd.endFrame(poses, eyeTextures);
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            System.out.println("reshape loc "+x+","+y+" size "+width+"x"+height);
            GL2 gl = drawable.getGL().getGL2();
            
            gl.glMatrixMode(GL2.GL_PROJECTION);
            for (int eye = 0; eye < 2; ++eye) {
                MatrixStack.PROJECTION.set(projections[eye]);
                gl.glMatrixMode(GL2.GL_PROJECTION);
                projectionDFB[eye] = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                MatrixStack.PROJECTION.top().fillFloatBuffer(projectionDFB[eye], true);
                projectionDFB[eye].rewind();
                gl.glLoadMatrixf(projectionDFB[eye]);            
            }

            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
            MatrixStack.MODELVIEW.set(player.invert());
            recenterView();
        }
    }   //end inner class
    
    private void recenterView() {
        Vector3f center = Vector3f.UNIT_Y.mult(eyeHeight);
       // Vector3f eye = new Vector3f(0, eyeHeight, ipd * 5.0f);
        Vector3f eye = new Vector3f(ipd*5.0f, eyeHeight, 0.0f);
        player = Matrix4f.lookat(eye, center, Vector3f.UNIT_Y).invert();
        hmd.recenterPose();
    }
    
    public final void drawPlaneXZ(final GL2 gl){
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_DECAL);
      float[] normal = new float[] {1f, 0f, 1f};
      float roomSize = 4.0f;
      float tileSize = 4.0f;      //if same then there are two tiles per square
        gl.glBegin(GL2.GL_QUADS);
          gl.glNormal3fv(normal, 0);
          gl.glColor4f(1f, 1f, 1f, 1f);
          gl.glTexCoord2f( 0f, 0f);                   gl.glVertex3f(-roomSize, 0f, -roomSize);
          gl.glTexCoord2f( tileSize, 0f);             gl.glVertex3f( roomSize, 0f, -roomSize);
          gl.glTexCoord2f( tileSize, tileSize);       gl.glVertex3f( roomSize, 0f, roomSize);
          gl.glTexCoord2f( 0f, tileSize);             gl.glVertex3f(-roomSize, 0f, roomSize);
        gl.glEnd();
    }
    
    public void run() {
        System.out.println("Startup");
        frameCount = -1;
        
        //step 1 - hmd init
        System.out.println("step 1 - hmd init");
        Hmd.initialize();
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
        
        //step 2 - hmd create
        System.out.println("step 2 - hmd create");
        hmd = Hmd.create(0);        //assume 1 device at index 0
        if (hmd == null) {
            System.out.println("null hmd");
            hmd = Hmd.createDebug(OvrLibrary.ovrHmdType.ovrHmd_DK2);
            return;
        }
        hmd.enableHswDisplay(false);
        
        //step 3 - hmd size queries
        System.out.println("step 3 - hmd sizes");
        OvrSizei resolution = hmd.Resolution;
        System.out.println("resolution= "+resolution.w+"x"+resolution.h);

        OvrSizei recommendedTex0Size = hmd.getFovTextureSize(OvrLibrary.ovrEyeType.ovrEye_Left, hmd.DefaultEyeFov[0], 1.0f);
        OvrSizei recommendedTex1Size = hmd.getFovTextureSize(OvrLibrary.ovrEyeType.ovrEye_Right, hmd.DefaultEyeFov[1], 1.0f);
        System.out.println("left= "+recommendedTex0Size.w+"x"+recommendedTex0Size.h);
        System.out.println("right= "+recommendedTex1Size.w+"x"+recommendedTex1Size.h);
        int displayW = recommendedTex0Size.w + recommendedTex1Size.w;
        int displayH = Math.max(recommendedTex0Size.h, recommendedTex1Size.h);
        OvrSizei renderTargetEyeSize = new OvrSizei(displayW / 2, displayH);   //size of single eye
        System.out.println("using eye size "+renderTargetEyeSize.w+"x"+renderTargetEyeSize.h);
        
        eyeRenderViewport[0].Pos = new OvrVector2i(0, 0);
        eyeRenderViewport[0].Size = renderTargetEyeSize;
        eyeRenderViewport[1].Pos = eyeRenderViewport[0].Pos;
        eyeRenderViewport[1].Size = renderTargetEyeSize;

        eyeTextures[0].Header = new TextureHeader(renderTargetEyeSize, eyeRenderViewport[0]);
        eyeTextures[1].Header = new TextureHeader(renderTargetEyeSize, eyeRenderViewport[1]);

        //step 4 - tracking
        System.out.println("step 4 - tracking");
        if (hmd.configureTracking(ovrTrackingCap_Orientation | ovrTrackingCap_Position, 0) == 0) {  //ovrTrackingCap_MagYawCorrection
            throw new IllegalStateException("Unable to start the sensor");
        }
        
        //step 5 - FOV
        System.out.println("step 5 - FOV");
        for (int eye = 0; eye < 2; ++eye) {
            fovPorts[eye] = hmd.DefaultEyeFov[eye];
            projections[eye] = RiftUtils.toMatrix4f(
                    Hmd.getPerspectiveProjection(
                        fovPorts[eye], 0.1f, 1000000f, true));
        }
        
        //step 6 - player params
        System.out.println("step 6 - player params");
        ipd = hmd.getFloat(OvrLibrary.OVR_KEY_IPD, ipd);
        eyeHeight = hmd.getFloat(OvrLibrary.OVR_KEY_EYE_HEIGHT, eyeHeight);
        recenterView();
        System.out.println("eyeheight="+eyeHeight);
        System.out.println("ipd="+ipd);
 
        //step 7 - opengl window
        System.out.println("step 7 - window");
        final Display display = NewtFactory.createDisplay("tiny");             
        final Screen screen  = NewtFactory.createScreen(display, 0);
        	screen.addReference();
        	MonitorDevice md0 = screen.getMonitorDevices().get(0);
        	MonitorDevice md1 = screen.getMonitorDevices().get(1);
    		System.out.println("0="+md0.getViewport().getWidth()+"x"+md0.getViewport().getHeight());
    		System.out.println("1="+md1.getViewport().getWidth()+"x"+md1.getViewport().getHeight());

        	List<MonitorDevice> rift = new ArrayList<MonitorDevice>();
        	rift.add(md1);
        
        GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        System.out.println("got: " + glProfile.getImplName());
        final Window window  = NewtFactory.createWindow(screen, new GLCapabilities(glProfile));
        window.setSize(displayW, displayH);
        glWindow = GLWindow.create(window);
        glWindow.setAutoSwapBufferMode(false);
        glWindow.setUndecorated(true);				//does not hit 75fps otherwise!
        	glWindow.setFullscreen(rift);
      //  glWindow.setFullscreen(true);		//only works on primary?
        glWindow.addKeyListener(this);
        glWindow.addGLEventListener(new DK2EventListener());
        
        glWindow.setVisible(true);
        NativeWindowFactoryImpl.addCustomShutdownHook(true, new Runnable(){
            public void run() {
                System.out.println("STOP");
                animator.stop();
            }});
        
        //step 8 - loop
        System.out.println("step 8 - loop");
        animator = new Animator();
        animator.add(glWindow);
        animator.start();
    }

    private void shutdown() {
        System.out.println("attempt shutdown");
        if (shutdownRunning.compareAndSet(false, true)) {
            try {
                System.out.println("doing SHUTDOWN");
                if (animator != null) {
                    System.out.println("animator.stop");
                    animator.stop();
                    animator = null;
                    System.out.println("animator.stop - done");
                }
                if (hmd != null) {
                    System.out.println("hmd.destroy & Hmd.shutdown");
                    hmd.destroy();
                    hmd = null;
                    Hmd.shutdown();
                    System.out.println("hmd.destroy & Hmd.shutdown - done");
                }
                if (glWindow != null) {
                    System.out.println("glWindow.destroy");
                    glWindow.destroy();
                    glWindow = null;
                    System.out.println("glWindow.destroy - done");
                }
            } finally {
                System.out.println("3");
                //switch mon
            }
        }
    }
    
    
// KEYBOARD =====================================
    public void keyPressed(KeyEvent e) {
    }

    static boolean hswDone = false;
    public void keyReleased(KeyEvent e) {
        System.out.println("hmd.getHSWDisplayState().Displayed="+hmd.getHSWDisplayState().Displayed);
    
        if (!hswDone && hmd.getHSWDisplayState().Displayed == 1) {
            hmd.dismissHSWDisplay();
            hswDone = true;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            shutdown();
        }
        if(e.getKeyCode() == KeyEvent.VK_F5) {
            new Thread() {
                public void run() {
                    glWindow.setFullscreen(!glWindow.isFullscreen());
            } }.start();
        }
        if(e.getKeyCode() == KeyEvent.VK_R) {
            recenterView();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new RiftClient().run();
    }
    
}
