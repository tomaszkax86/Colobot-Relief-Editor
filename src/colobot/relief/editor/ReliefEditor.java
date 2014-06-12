/*
    Copyright (c) 2014 Tomasz Kapuściński
*/
package colobot.relief.editor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class ReliefEditor extends JPanel implements MouseListener, MouseMotionListener, ActionListener
{
    // UI elements
    private final JMenuBar menubar = new JMenuBar();
    private final JMenuItem newMenuItem = new JMenuItem("New");
    private final JMenuItem openMenuItem = new JMenuItem("Open");
    private final JMenuItem reopenMenuItem = new JMenuItem("Reopen");
    private final JMenuItem saveMenuItem = new JMenuItem("Save");
    private final JMenuItem saveAsMenuItem = new JMenuItem("Save as");
    private final JMenuItem exitMenuItem = new JMenuItem("Exit");
    
    private final JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
    
    private File currentFile = null;
    private BufferedImage relief = null;
    private float scale = 4.0f;
    private float centerX = 0f, centerY = 0f;
    private int change = 1;
    
    private int oldX, oldY;
    private boolean dragging = false;
    
    private int selectedX, selectedY;
    
    
    public ReliefEditor()
    {
        initComponents();
    }
    
    public JMenuBar getJMenuBar()
    {
        return menubar;
    }
    
    public BufferedImage getRelief()
    {
        return relief;
    }
    
    public void setRelief(BufferedImage relief)
    {
        this.relief = relief;
        
        repaint();
    }
    
    public int getChange()
    {
        return change;
    }
    
    public void setChange(int change)
    {
        this.change = change;
    }
    
    public void addChange(int dChange)
    {
        this.change += dChange;
    }
    
    public void addScale(float dScale)
    {
        this.scale += dScale;
        
        repaint();
    }
    
    public int getSelectedX()
    {
        return selectedX;
    }
    
    public int getSelectedY()
    {
        return selectedY;
    }
    
    @Override
    public void paintComponent(Graphics gr)
    {
        Graphics2D g = (Graphics2D) gr;
        
        int width = getWidth();
        int height = getHeight();
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        
        if(relief == null) return;
        
        g.translate(centerX, centerY);
        g.scale(scale, scale);
        g.drawImage(relief, 0, 0, 161, 161, 0, 0, 161, 161, this);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        changePixel(e.getX(), e.getY(), change);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if(e.getButton() == MouseEvent.BUTTON3)
        {
            oldX = e.getX();
            oldY = e.getY();
            
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if(e.getButton() == MouseEvent.BUTTON3)
        {
            dragging = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if(dragging)
        {
            int x = e.getX();
            int y = e.getY();
            
            int dx = x - oldX;
            int dy = y - oldY;
            
            centerX += dx;
            centerY += dy;
            
            oldX = x;
            oldY = y;
            
            repaint();
        }
        else
        {
            changePixel(e.getX(), e.getY(), change);
        }
        
        selectedX = toImageX(e.getX());
        selectedY = toImageY(e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        selectedX = toImageX(e.getX());
        selectedY = toImageY(e.getY());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        
        if(source == newMenuItem)
        {
            relief = new BufferedImage(161, 161, BufferedImage.TYPE_BYTE_GRAY);
        }
        else if(source == openMenuItem)
        {
            open(null);
        }
        else if(source == reopenMenuItem)
        {
            open(currentFile);
        }
        else if(source == saveMenuItem)
        {
            save(currentFile);
        }
        else if(source == saveAsMenuItem)
        {
            save(null);
        }
        else if(source == exitMenuItem)
        {
            // TODO: complete code
        }
    }
    
    private void open(File file)
    {
        if(file == null)
        {
            int result = fileChooser.showOpenDialog(this);
            if(result != JFileChooser.APPROVE_OPTION) return;

            file = fileChooser.getSelectedFile();
        }
        
        try
        {
            BufferedImage image = ImageIO.read(file);

            if(image.getWidth() != 161) throw new RuntimeException("Invalid image width (not 161)");
            if(image.getHeight() != 161) throw new RuntimeException("Invalid image height (not 161)");

            relief = image;
            currentFile = file;
            
            repaint();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex.getLocalizedMessage());
        }
    }
    
    private void save(File file)
    {
        if(relief == null)
        {
            JOptionPane.showMessageDialog(this, "Nothing to save");
            return;
        }

        if(file == null)
        {
            int result = fileChooser.showSaveDialog(this);
            if(result != JFileChooser.APPROVE_OPTION) return;

            file = fileChooser.getSelectedFile();
        }
        
        try
        {
            String format = file.getName();
            int index = format.lastIndexOf('.');

            format = format.substring(index + 1);
            System.out.println(format);

            ImageIO.write(relief, format, file);
            
            currentFile = file;
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex.getLocalizedMessage());
        }
    }
    
    private void initComponents()
    {
        JMenu menu = new JMenu("File");
        menu.add(newMenuItem);
        menu.add(openMenuItem);
        menu.add(reopenMenuItem);
        menu.add(saveMenuItem);
        menu.add(saveAsMenuItem);
        menu.add(new JSeparator());
        menu.add(exitMenuItem);
        menubar.add(menu);
        
        newMenuItem.addActionListener(this);
        openMenuItem.addActionListener(this);
        reopenMenuItem.addActionListener(this);
        saveMenuItem.addActionListener(this);
        saveAsMenuItem.addActionListener(this);
        exitMenuItem.addActionListener(this);
        
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }
    
    public void changePixel(int dv)
    {
        if(relief == null) return;
        
        int x = selectedX;
        int y = selectedY;
        
        if(x < 0 || x > 160) return;
        if(y < 0 || y > 160) return;
        
        int v = clamp((0xFF & relief.getRGB(x, y)) - dv, 0, 255);
        
        int color = v | (v << 8) | (v << 16) | (0xFF << 24);
        
        relief.setRGB(x, y, color);
        repaint();
    }
    
    public void changePixel(int x, int y, int dv)
    {
        if(relief == null) return;
        
        x = toImageX(x);
        y = toImageY(y);
        
        if(x < 0 || x > 160) return;
        if(y < 0 || y > 160) return;
        
        int v = clamp((0xFF & relief.getRGB(x, y)) - dv, 0, 255);
        
        int color = v | (v << 8) | (v << 16) | (0xFF << 24);
        
        relief.setRGB(x, y, color);
        repaint();
    }
    
    public int toImageX(int mouseX)
    {
        return (int) ((mouseX - centerX) / scale);
    }
    
    public int toImageY(int mouseY)
    {
        return (int) ((mouseY - centerY) / scale);
    }
    
    public static int clamp(int value, int min, int max)
    {
        return Math.min(max, Math.max(value, min));
    }
}
