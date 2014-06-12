/*
    Copyright (c) 2014 Tomasz Kapuściński
*/
package colobot.relief.editor;

import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.util.glu.GLU.*;

public class Editor implements Runnable, WindowListener
{
    private JFrame window;
    private ReliefEditor editor;
    private AWTGLCanvas canvas;
    private boolean exit = false;
    private boolean rendering = false;
    
    private ByteBuffer vertices;
    private ShortBuffer elements;
    private FloatBuffer floats;
    private int[] textures;
    private int texture = 0;
    
    private float scale = 1.0f;
    private float water = 30.0f;
    private float x, y, z;
    private float pitch, yaw;
    private float speed = 1.0f;
    
    private int counter = 0;
    private int counterMax = 30;
    
    private boolean captured = false;
    private boolean keyUp = false;
    private boolean keyDown = false;
    private boolean keyLeft = false;
    private boolean keyRight = false;
    
    private boolean lines = false;
    
    
    @Override
    public void run()
    {
        try
        {
            doRun();
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
    
    private void doRun() throws Exception
    {
        init();
        
        while(!exit)
        {
            update();
            render();
        }
        
        release();
    }
    
    private void init() throws Exception
    {
        window = new JFrame("Colobot Relief Editor");
        editor = new ReliefEditor();
        canvas = new AWTGLCanvas();
        
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(editor);
        panel.add(canvas);
        window.add(panel);
        
        window.setJMenuBar(editor.getJMenuBar());
        
        window.setSize(800, 600);
        window.addWindowListener(this);
        window.setVisible(true);
        
        Display.setParent(canvas);
        Display.setVSyncEnabled(true);
        Display.create();
        
        glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
        
        x = 80;
        y = 150;
        z = 80;
        
        pitch = 0;
        yaw = 180;
        
        vertices = BufferUtils.createByteBuffer(161 * 161 * 8 * 4);
        elements = BufferUtils.createShortBuffer(6 * 160 * 160);
        floats = BufferUtils.createFloatBuffer(12);
        
        for(int j=0; j<161; j++)
        {
            for(int i=0; i<161; i++)
            {
                vertices.putFloat(5*i).putFloat(0).putFloat(5*j);
                vertices.putFloat(i).putFloat(j);
                vertices.putFloat(0).putFloat(1).putFloat(0);
                
                setHeight(i, j, 0);
            }
        }
        
        vertices.flip();
        
        for(int i=0; i<160; i++)
        {
            for(int j=0; j<160; j++)
            {
                elements.put(getVertexIndex(i  , j  ));
                elements.put(getVertexIndex(i  , j+1));
                elements.put(getVertexIndex(i+1, j+1));
                
                elements.put(getVertexIndex(i+1, j+1));
                elements.put(getVertexIndex(i+1, j  ));
                elements.put(getVertexIndex(i  , j  ));
            }
        }
        
        elements.flip();
        
        ByteBuffer buffer = BufferUtils.createByteBuffer(4 * 1024 * 1024);
        
        // loading textures
        
        File dir = new File("textures");
        File[] files = dir.listFiles();
        
        if(files.length == 0)
        {
            textures = new int[1];

            // default texture
            buffer.clear();

            buffer.putInt(0xFFFFFFFF);
            buffer.putInt(0xFF000000);
            buffer.putInt(0xFF000000);
            buffer.putInt(0xFFFFFFFF);

            buffer.flip();

            textures[0] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textures[0]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2, 2, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        else
        {
            textures = new int[files.length];

            for(int i=0; i<files.length; i++)
            {
                textures[i] = loadTexture(buffer, files[i]);
            }
        }
    }
    
    private void release()
    {
        Display.destroy();
    }
    
    private void update()
    {
        Display.sync(60);
        Display.update();
        
        processInput();
        
        updateHeightMap();
    }
    
    private void processInput()
    {
        float dPitch = 0, dYaw = 0;
        
        while(Keyboard.next())
        {
            int key = Keyboard.getEventKey();
            boolean state = Keyboard.getEventKeyState();
            
            switch(key)
            {
                case Keyboard.KEY_W:
                    keyUp = state;
                    break;
                case Keyboard.KEY_S:
                    keyDown = state;
                    break;
                case Keyboard.KEY_A:
                    keyLeft = state;
                    break;
                case Keyboard.KEY_D:
                    keyRight = state;
                    break;
                case Keyboard.KEY_EQUALS:
                    editor.addChange(1);
                    break;
                case Keyboard.KEY_MINUS:
                    editor.addChange(-1);
                    break;
                case Keyboard.KEY_E:
                    if(state)
                        texture = (texture + 1) % textures.length;
                    break;
                case Keyboard.KEY_ESCAPE:
                    if(state)
                    {
                        captured = !captured;
                        Mouse.setGrabbed(captured);
                    }
                    break;
                case Keyboard.KEY_F1:
                    lines = false;
                    break;
                case Keyboard.KEY_F2:
                    lines = true;
                    break;
            }
        }
        
        if(keyUp) dPitch -= 1.0f;
        if(keyDown) dPitch += 1.0f;
        if(keyLeft) dYaw -= 1.0f;
        if(keyRight) dYaw += 1.0f;
        
        while(Mouse.next())
        {
            int dx = Mouse.getEventDX();
            int dy = Mouse.getEventDY();
            
            if(captured)
            {
                dPitch -= 0.25f * dy;
                dYaw += 0.25f * dx;
            }
            
            int wheel = Mouse.getEventDWheel();
            
            if(wheel != 0)
            {
                if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                {
                    speed += wheel > 0 ? 0.25f : -0.25f;
                    
                    if(speed < -10.0f) speed = -10.0f;
                    if(speed >  10.0f) speed =  10.0f;
                }
                else if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
                {
                    editor.changePixel(wheel > 0 ? 1 : -1);
                }
                else
                {
                    float dScale = 0.0125f * wheel;

                    editor.addScale(dScale);
                }
            }
        }
        
        pitch += dPitch;
        yaw += dYaw;
        
        if(Keyboard.isKeyDown(Keyboard.KEY_SPACE))
        {
            float dy = (float) -Math.sin(Math.toRadians(pitch));
            float dv = (float)  Math.cos(Math.toRadians(pitch));
            float dx = (float)  Math.sin(Math.toRadians(yaw)) * dv;
            float dz = (float) -Math.cos(Math.toRadians(yaw)) * dv;
            
            x += dx * speed;
            y += dy * speed;
            z += dz * speed;
        }
    }
    
    private void render()
    {
        if(!rendering) return;
        
        int width = Display.getWidth();
        int height = Display.getHeight();
        float aspect = (float)width / (float)height;
        
        glViewport(0, 0, width, height);
        
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(60.0f, aspect, 0.1f, 250.0f);
        
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glRotatef(pitch, 1, 0, 0);
        glRotatef(yaw, 0, 1, 0);
        glTranslatef(-x, -y, -z);
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glColor3f(1, 1, 1);
        
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        
        // change to wireframe rendering
        if(lines)
        {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glDisable(GL_CULL_FACE);
            glLineWidth(1.0f);
        }
        
        floats.clear();
        floats.put(0.5f).put(0.5f).put(0).put(0);
        floats.flip();
        glLight(GL_LIGHT0, GL_POSITION, floats);
        
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textures[texture]);
        
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        
        glVertexPointer(3, GL_FLOAT, 8*4, (ByteBuffer) vertices.position(0));
        glTexCoordPointer(2, GL_FLOAT, 8*4, (ByteBuffer) vertices.position(3*4));
        glNormalPointer(GL_FLOAT, 8*4, (ByteBuffer) vertices.position(5*4));
        
        glDrawElements(GL_TRIANGLES, elements);
        
        glDisableClientState(GL_NORMAL_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        
        glDisable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        // go back to normal rendering
        if(lines)
        {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
        
        // render "cursor"
        int sx = editor.getSelectedX();
        int sy = editor.getSelectedY();
        
        do
        {
            if(sx < 0 || sx > 160) break;
            if(sy < 0 || sy > 160) break;

            glDisable(GL_LIGHTING);

            glLineWidth(2.0f);

            glColor3f(1.0f, 1.0f, 1.0f);

            float x = 5.0f * sx;
            float y = 5.0f * sy;

            glBegin(GL_LINES);

                glVertex3f(x, 0, y);
                glVertex3f(x, 200, y);

            glEnd();
        }
        while(false);
        
        glEnable(GL_LIGHTING);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        
        // render water surface
        glColor4f(0.1f, 0.2f, 0.8f, 0.5f);
        
        glBegin(GL_QUADS);
        
            glVertex3f(    0, 0,     0);
            glVertex3f(5*161, 0,     0);
            glVertex3f(5*161, 0, 5*161);
            glVertex3f(    0, 0, 5*161);
        
        glEnd();
        
        glDisable(GL_BLEND);
        glDepthMask(true);
    }
    
    @Override
    public void windowOpened(WindowEvent e)
    {
        rendering = true;
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        exit = true;
    }

    @Override
    public void windowClosed(WindowEvent e)
    {
        
    }

    @Override
    public void windowIconified(WindowEvent e)
    {
        rendering = false;
    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {
        rendering = true;
    }

    @Override
    public void windowActivated(WindowEvent e)
    {
        
    }

    @Override
    public void windowDeactivated(WindowEvent e)
    {
        
    }
    
    private void updateHeightMap()
    {
        BufferedImage image = editor.getRelief();
        
        // update height values
        if(image != null)
        {
            for(int i=0; i<161; i++)
            {
                for(int j=0; j<161; j++)
                {
                    float value = 0.25f * scale * (255 - (0xFF & image.getRGB(i, j))) - water;

                    setHeight(i, j, value);
                }
            }
        }
        else
        {
            for(int i=0; i<161; i++)
            {
                for(int j=0; j<161; j++)
                {
                    setHeight(i, j, 0.0f);
                }
            }
        }
        
        // update normals -- every 100 frame
        counter++;
        
        if(counter == counterMax)
        {
            for(int i=1; i<160; i++)
            {
                for(int j=1; j<160; j++)
                {
                    float h = getHeight(i, j);
                    float x1 = h - getHeight(i-1, j);
                    float x2 = h - getHeight(i+1, j);
                    float y1 = h - getHeight(i, j-1);
                    float y2 = h - getHeight(i, j+1);
                    
                    float nx = x1 - x2;
                    float ny = 5;
                    float nz = y1 - y2;
                    
                    float dist = 1 / (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
                    
                    nx *= dist;
                    ny *= dist;
                    nz *= dist;
                    
                    setNormal(i, j, nx, ny, nz);
                }
            }
            
            counter = 0;
        }
    }
    
    private short getVertexIndex(int x, int y)
    {
        return (short)(x + 161 * y);
    }
    
    private int getVertexOffset(int x, int y)
    {
        return (8 * 4) * (x + 161 * y);
    }
    
    private float getHeight(int x, int y)
    {
        int index = getVertexOffset(x, y) + 4;
        
        return vertices.getFloat(index);
    }
    
    private void setHeight(int x, int y, float value)
    {
        int index = getVertexOffset(x, y) + 4;
        
        vertices.putFloat(index, value);
    }
    
    private void setNormal(int x, int y, float nx, float ny, float nz)
    {
        int index = getVertexOffset(x, y) + 20;
        
        vertices.putFloat(index+0, nx);
        vertices.putFloat(index+4, ny);
        vertices.putFloat(index+8, nz);
    }

    private int loadTexture(ByteBuffer buffer, File file)
    {
        try
        {
            BufferedImage image = ImageIO.read(file);

            int width = image.getWidth();
            int height = image.getHeight();
            
            buffer.clear();
            
            for(int j=height-1; j>=0; j--)
            {
                for(int i=0; i<width; i++)
                {
                    buffer.putInt(image.getRGB(i, j));
                }
            }
            
            buffer.flip();
            
            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, buffer);

            glBindTexture(GL_TEXTURE_2D, 0);
            return id;
        }
        catch(Exception e)
        {
            return 0;
        }
    }
}
