/*
    Copyright (c) 2014 Tomasz Kapuściński
*/
package colobot.relief.editor;

import java.io.File;
import org.lwjgl.LWJGLUtil;

public class MainClass
{
    public static void main(String[] args)
    {
        System.setProperty("org.lwjgl.librarypath", System.getProperty("user.dir")
            + File.separator + "native" + File.separator + LWJGLUtil.getPlatformName());
        
        new Editor().run();
        
        System.exit(0);
    }
}
