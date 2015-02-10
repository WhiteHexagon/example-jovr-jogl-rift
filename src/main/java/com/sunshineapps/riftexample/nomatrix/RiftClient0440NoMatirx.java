package com.sunshineapps.riftexample.nomatrix;

import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_Chromatic;
import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_TimeWarp;
import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.ovrDistortionCap_Vignette;
import static com.oculusvr.capi.OvrLibrary.ovrEyeType.ovrEye_Count;
import static com.oculusvr.capi.OvrLibrary.ovrEyeType.ovrEye_Left;
import static com.oculusvr.capi.OvrLibrary.ovrEyeType.ovrEye_Right;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_MagYawCorrection;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Orientation;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.ovrTrackingCap_Position;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLLightingFunc;

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.oculusvr.capi.EyeRenderDesc;
import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.GLTexture;
import com.oculusvr.capi.GLTextureData;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrQuaternionf;
import com.oculusvr.capi.OvrRecti;
import com.oculusvr.capi.OvrSizei;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.RenderAPIConfig;
import com.oculusvr.capi.TextureHeader;
import com.sunshineapps.riftexample.thirdparty.FixedTexture;
import com.sunshineapps.riftexample.thirdparty.FixedTexture.BuiltinTexture;
import com.sunshineapps.riftexample.thirdparty.FrameBuffer;

public final class RiftClient0440NoMatirx implements KeyListener {
    private final AtomicBoolean shutdownRunning = new AtomicBoolean(false);
    private final boolean useDebugHMD = true;
    
    // JOGL
    private Animator animator;
    private GLWindow glWindow;

    // Rift Specific
    private Hmd hmd;
    private int frameCount = -1;
    private final OvrVector3f eyeOffsets[] = (OvrVector3f[]) new OvrVector3f().toArray(2);
    private final OvrRecti[] eyeRenderViewport = (OvrRecti[]) new OvrRecti().toArray(2);
    private final Posef poses[] = (Posef[]) new Posef().toArray(2);
    private final GLTexture eyeTextures[] = (GLTexture[]) new GLTexture().toArray(2);
    private final FovPort fovPorts[] = (FovPort[]) new FovPort().toArray(2);
    private float ipd = OvrLibrary.OVR_DEFAULT_IPD;
    private float eyeHeight = OvrLibrary.OVR_DEFAULT_EYE_HEIGHT;
    
    //FPS
    private final int fpsReportingPeriodSeconds = 5;
    private final ScheduledExecutorService fpsCounter = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger frames = new AtomicInteger(0);
    private final AtomicInteger fps = new AtomicInteger(0);
    Runnable fpsJob = new Runnable() {
        public void run() {
            int frameCount = frames.getAndSet(0);
            fps.set(frameCount/fpsReportingPeriodSeconds);
            frames.addAndGet(frameCount-(fps.get()*fpsReportingPeriodSeconds));
            System.out.println(frameCount+" frames in "+fpsReportingPeriodSeconds+"s. "+fps.get()+"fps");
        }
    };

    private final class DK2EventListener implements GLEventListener {
        private FrameBuffer eyeDFB[];        
        private FixedTexture cheq;
        
        float canvasRatio;

        public DK2EventListener() {
        }

        public void init(GLAutoDrawable drawable) {
            final GL2 gl = drawable.getGL().getGL2();
            gl.glClearColor(.42f, .67f, .87f, 1f);

            // Lighting
            gl.glEnable(GLLightingFunc.GL_LIGHTING);
            gl.glEnable(GLLightingFunc.GL_LIGHT0);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            float[] noAmbient = { 0.2f, 0.2f, 0.2f, 1f };
            float[] spec = { 1f, 1f, 1f, 1f };
            float[] diffuse = { 1f, 1f, 1f, 1f };
            float[] lightPos = { 0.5f, 0.0f, 1.0f, 0.0001f};
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
            rc.Header.BackBufferSize = hmd.Resolution;
            rc.Header.Multisample = 1;
            int distortionCaps = ovrDistortionCap_Chromatic | ovrDistortionCap_TimeWarp | ovrDistortionCap_Vignette;
            EyeRenderDesc eyeRenderDescs[] = hmd.configureRendering(rc, distortionCaps, fovPorts);
            for (int eye = 0; eye < 2; ++eye) {
                eyeOffsets[eye].x = eyeRenderDescs[eye].HmdToEyeViewOffset.x;
                eyeOffsets[eye].y = eyeRenderDescs[eye].HmdToEyeViewOffset.y;
                eyeOffsets[eye].z = eyeRenderDescs[eye].HmdToEyeViewOffset.z;
            }

            eyeDFB = new FrameBuffer[2];
            eyeDFB[ovrEye_Left] = new FrameBuffer(gl, eyeRenderViewport[ovrEye_Left].Size);
            eyeDFB[ovrEye_Right] = new FrameBuffer(gl, eyeRenderViewport[ovrEye_Right].Size);

            eyeTextures[ovrEye_Left].ogl.TexId = eyeDFB[ovrEye_Left].getTextureId();
            eyeTextures[ovrEye_Right].ogl.TexId = eyeDFB[ovrEye_Right].getTextureId();

            // scene prep
            gl.glEnable(GL2.GL_TEXTURE_2D);
            cheq = new FixedTexture(gl, BuiltinTexture.tex_checker);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
            
            //fps
            fpsCounter.scheduleAtFixedRate(fpsJob, 0, fpsReportingPeriodSeconds, TimeUnit.SECONDS); 
        }

        public void dispose(GLAutoDrawable drawable) {
            // TODO Auto-generated method stub
        }

        public void display(GLAutoDrawable drawable) {
            // System.out.println("Display "+frameCount);
            hmd.beginFrameTiming(++frameCount);
            GL2 gl = drawable.getGL().getGL2();
            Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
            for (int eyeIndex = 0; eyeIndex < ovrEye_Count; eyeIndex++) {
                int eye = hmd.EyeRenderOrder[eyeIndex];
                Posef pose = eyePoses[eye];
                poses[eye].Orientation = pose.Orientation;
                poses[eye].Position = pose.Position;

                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, eyeDFB[eye].getId());
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

                //Projection
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();       
                gl.glFrustum(-1.0f * canvasRatio, canvasRatio, -1.0f, 1.0f, 3f, 25000.0f);

                //ModelView
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glLoadIdentity();
                gl.glPushMatrix();
                
                OvrQuaternionf q = poses[eye].Orientation;
                double yaw;
                double roll;
                double pitch;
                float test = q.x*q.y + q.z*q.w;
                if (test > 0.499) { // singularity at north pole
                    yaw = 2 * Math.atan2(q.x, q.w);
                    roll = Math.PI/2;
                    pitch = 0f;
                } else  if (test < -0.499) { // singularity at south pole
                    yaw = -2 * Math.atan2(q.x, q.w);
                    roll = -Math.PI/2;
                    pitch = 0;
                } else {
                    double sqx = q.x * q.x;
                    double sqy = q.y * q.y;
                    double sqz = q.z * q.z;
                    yaw = Math.atan2(2*q.y*q.w-2*q.x*q.z, 1 - 2*sqy - 2*sqz);
                    roll = Math.asin(2*test);
                    pitch = Math.atan2(2*q.x*q.w-2*q.y*q.z, 1 - 2*sqx - 2*sqz);
                }
                gl.glRotatef((float)Math.toDegrees(-pitch), 1.0f, 0.0f, 0.0f);
                gl.glRotatef((float)Math.toDegrees(-roll), 0.0f, 0.0f, 1.0f);
                gl.glRotatef((float)Math.toDegrees(-yaw), 0.0f, 1.0f, 0.0f);

                gl.glColor3f(1.0f, 0.0f, 0.0f);
                new GLUT().glutWireSphere(160, 16, 16);
                {
                    gl.glTranslatef(-poses[eye].Position.x, -poses[eye].Position.y - eyeHeight, -poses[eye].Position.z);
                    
                    // tiles on floor
                    gl.glEnable(GL2.GL_TEXTURE_2D);
                    gl.glBindTexture(GL2.GL_TEXTURE_2D, cheq.getId());
                    gl.glTranslatef(0.0f, -eyeHeight, 0.0f);
                    drawPlaneXZ(gl);
                    gl.glTranslatef(0.0f, eyeHeight, 0.0f);
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                gl.glPopMatrix();
            }
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
            gl.glDisable(GL2.GL_TEXTURE_2D);

            frames.incrementAndGet();
            hmd.endFrame(poses, eyeTextures);
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            System.out.println("reshape loc " + x + "," + y + " size " + width + "x" + height);
            canvasRatio = (float)width/(float)height;
        }
    } // end inner class
    
    private void recenterView() {
        hmd.recenterPose();
    }

    public final void drawPlaneXZ(final GL2 gl) {
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_DECAL);
        float[] normal = new float[] { 0f, 1f, 0f };
        float roomSize = 4.0f;
        float tileSize = 4.0f; // if same then there are two tiles per square
        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glNormal3fv(normal, 0);
            gl.glColor4f(1f, 1f, 1f, 1f);
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex3f(-roomSize, 0f, roomSize);
            gl.glTexCoord2f(tileSize, 0f);
            gl.glVertex3f(roomSize, 0f, roomSize);
            gl.glTexCoord2f(tileSize, tileSize);
            gl.glVertex3f(roomSize, 0f, -roomSize);
            gl.glTexCoord2f(0f, tileSize);
            gl.glVertex3f(-roomSize, 0f, -roomSize);
        }
        gl.glEnd();
    }

    public void run() {
    	System.out.println(""+System.getProperty("java.version"));
        // step 1 - hmd init
        System.out.println("step 1 - hmd init");
        Hmd.initialize();
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        // step 2 - hmd create
        System.out.println("step 2 - hmd create");
        hmd = Hmd.create(0); // assume 1 device at index 0
        if (hmd == null) {
            System.out.println("null hmd");
            hmd = Hmd.createDebug(OvrLibrary.ovrHmdType.ovrHmd_DK2);
            if (!useDebugHMD) {
                return;
            }
        }

        // step 3 - hmd size queries
        System.out.println("step 3 - hmd sizes");
        OvrSizei resolution = hmd.Resolution;
        System.out.println("resolution= " + resolution.w + "x" + resolution.h);

        OvrSizei recommendedTex0Size = hmd.getFovTextureSize(OvrLibrary.ovrEyeType.ovrEye_Left, hmd.DefaultEyeFov[0], 1.0f);
        OvrSizei recommendedTex1Size = hmd.getFovTextureSize(OvrLibrary.ovrEyeType.ovrEye_Right, hmd.DefaultEyeFov[1], 1.0f);
        System.out.println("left= " + recommendedTex0Size.w + "x" + recommendedTex0Size.h);
        System.out.println("right= " + recommendedTex1Size.w + "x" + recommendedTex1Size.h);
        int displayW = recommendedTex0Size.w + recommendedTex1Size.w;
        int displayH = Math.max(recommendedTex0Size.h, recommendedTex1Size.h);
        OvrSizei renderTargetEyeSize = new OvrSizei(displayW / 2, displayH);    //single eye
        System.out.println("using eye size " + renderTargetEyeSize.w + "x" + renderTargetEyeSize.h);

        eyeRenderViewport[0].Pos = new OvrVector2i(0, 0);
        eyeRenderViewport[0].Size = renderTargetEyeSize;
        eyeRenderViewport[1].Pos = eyeRenderViewport[0].Pos;
        eyeRenderViewport[1].Size = renderTargetEyeSize;

        eyeTextures[0].ogl = new GLTextureData(new TextureHeader(renderTargetEyeSize, eyeRenderViewport[0]));
        eyeTextures[1].ogl = new GLTextureData(new TextureHeader(renderTargetEyeSize, eyeRenderViewport[1]));

        // step 4 - tracking
        System.out.println("step 4 - tracking");
        if (hmd.configureTracking(ovrTrackingCap_Orientation | ovrTrackingCap_MagYawCorrection | ovrTrackingCap_Position, 0) == 0) {
            throw new IllegalStateException("Unable to start the sensor");
        }

        // step 5 - FOV
        System.out.println("step 5 - FOV");
        for (int eye = 0; eye < 2; ++eye) {
            fovPorts[eye] = hmd.DefaultEyeFov[eye];
        }

        // step 6 - player params
        System.out.println("step 6 - player params");
        ipd = hmd.getFloat(OvrLibrary.OVR_KEY_IPD, ipd);
        eyeHeight = hmd.getFloat(OvrLibrary.OVR_KEY_EYE_HEIGHT, eyeHeight);
        recenterView();
        System.out.println("eyeheight=" + eyeHeight + " ipd=" + ipd);

        // step 7 - opengl window
        System.out.println("step 7 - window");
        final Display display = NewtFactory.createDisplay("tiny");
        final Screen screen = NewtFactory.createScreen(display, 0);
        screen.addReference();
        final List<MonitorDevice> riftMonitor = new ArrayList<MonitorDevice>();
        for (MonitorDevice monitor : screen.getMonitorDevices()) {
            if (monitor.getViewport().getWidth() == resolution.w && monitor.getViewport().getHeight() == resolution.h) {
                riftMonitor.add(monitor);
                System.out.println("Found Rift Monitor");
                break;
            }
        }
        if (riftMonitor.size() != 1) { // could be multiple?
            System.out.println("Cant find Rift for fullscreen mode - check resolution lookup");
            if (useDebugHMD) {
                riftMonitor.add(screen.getMonitorDevices().get(0));
            } else {
                return;
            }
        }

        GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        System.out.println("got: " + glProfile.getImplName());
        final Window window = NewtFactory.createWindow(screen, new GLCapabilities(glProfile));
        window.setSize(displayW, displayH);
        glWindow = GLWindow.create(window);
        glWindow.setAutoSwapBufferMode(false);
        glWindow.setUndecorated(true); // does not hit 75fps otherwise!
        glWindow.setFullscreen(riftMonitor);
        // glWindow.setFullscreen(true); //only works on primary?
        glWindow.addKeyListener(this);
        glWindow.addGLEventListener(new DK2EventListener());
        glWindow.setVisible(true);

        // step 8 - animator loop
        System.out.println("step 8 - animator loop");
        animator = new Animator();
        animator.add(glWindow);
        animator.start();
    }

    private void shutdown() {
        System.out.println("attempt shutdown");
        if (shutdownRunning.compareAndSet(false, true)) {
            try {
                System.out.println("doing SHUTDOWN");
                fpsCounter.shutdown();
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
                System.out.println("done.");
            }
        }
    }

    // KEYBOARD =====================================
    public void keyPressed(KeyEvent e) {
    }

    volatile boolean hswDone = false;

    public void keyReleased(KeyEvent e) {
        System.out.println("hmd.getHSWDisplayState().Displayed=" + hmd.getHSWDisplayState().Displayed);
        if (!hswDone && hmd.getHSWDisplayState().Displayed == 1) {
            hmd.dismissHSWDisplay();
            hswDone = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            shutdown();
        }
        if (e.getKeyCode() == KeyEvent.VK_R) {
            recenterView();
        }
    }

    public static void main(String[] args) {
        new RiftClient0440NoMatirx().run();
    }

}
