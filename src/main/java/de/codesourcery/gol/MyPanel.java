/**
 * Copyright 2015 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.gol;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class MyPanel extends JPanel 
{
    private static final Point tmp=new Point();
    
    private final QuadTree tree;
    
    private boolean renderGrid=true;
    
    private int quadTreeLevelToRender = 0;
    
    private int simulationSpeed=1;
    private boolean advanceSimulation;
    private boolean autoScale = false;
    
    private final VisibleArea viewPort = new VisibleArea();
    
    private boolean repaintNeeded = true;
    
    private float deltaSeconds;
    
    protected final class VisibleArea 
    {
        public int x0;
        public int y0;
        
        public int x1;
        public int y1;
        
        private float scaleX;
        private float scaleY;
        
        public void set(int x0,int y0,int width,int height) 
        {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x0 + width;
            this.y1 = y0 + height;
            System.out.println("Viewport: ("+x0+","+y0+") -> ("+x1+","+y1+")");
            recalculateScaling();
            repaintNeeded = true;
        }
        
        public void recalculateScaling() {
            scaleX = getWidth() / (float) width();
            scaleY = getHeight() / (float) height();     
        }
        
        public void autoScale() 
        {
            if ( tree.root != null ) 
            {
                this.x0 = tree.xTopLeft;
                this.y0 = tree.yTopLeft;
                this.x1 = tree.xTopLeft + tree.root.size;
                this.y1 = tree.yTopLeft + tree.root.size;
                recalculateScaling();
            }
        }
        
        public int width() {
            return x1-x0;
        }
        
        public int height() {
            return y1-y0;
        }
        
        public void translate(int dx,int dy) 
        {
            this.x0 += dx;
            this.x1 += dx;
            this.y0 += dy;
            this.y1 += dy;
            repaintNeeded=true;
        }
        
        public void zoomIn(int pixels) 
        {
            final float aspectRatio = height() / (float) width();
            
            int newX0 = (int) (x0 + pixels);
            int newY0 = (int) (y0 + pixels*aspectRatio);
            
            int newX1 = (int) ( x1 - pixels);
            int newY1 = (int) ( y1 - pixels*aspectRatio);
            
            if ( newX1 > newX0 && newY1 > newY0 ) 
            {
                x0 = newX0;
                y0 = newY0;
                x1 = newX1;
                y1 = newY1;
                repaintNeeded = true;
                recalculateScaling();
            }
        }
        
        public void zoomOut(int pixels) 
        {
            final float aspectRatio = height() / (float) width();
            
            x0 -= (int) (pixels);
            y0 -= (int) (pixels*aspectRatio);
            
            x1 += (int) (pixels);
            y1 += (int) (pixels*aspectRatio);
            recalculateScaling();
            repaintNeeded = true;
        }        
        
        public void modelToView(Point p) 
        {
            p.x = Math.round( (p.x-x0)*scaleX);
            p.y = Math.round( (p.y-y0)*scaleY);
        }
        
        public void viewToModel(Point p) 
        {
            p.x = Math.round( (p.x/scaleX)+x0);
            p.y = Math.round( (p.y/scaleY)+y0);
        }
    }
    
    public boolean tick(float deltaSeconds) 
    {
        this.deltaSeconds = deltaSeconds;
        
        boolean needRepaint = this.repaintNeeded;
        this.repaintNeeded = false;
        if ( advanceSimulation ) 
        {
            for ( int i = 0,max = simulationSpeed ; i < max ; i++ ) {
                tree.nextGeneration();
            }
            needRepaint = true;
        }
        return needRepaint;
    }
    
    public MyPanel()
    {
        this.tree = new QuadTree(1);
        setBackground( Color.WHITE );
        setFocusable( true );
        setRequestFocusEnabled(true);
        requestFocus();
        reset();
    }
    
    protected void paintComponent(java.awt.Graphics g) 
    {
        super.paintComponent(g);
        
        renderQuadTree(g);
        
        if ( renderGrid ) 
        {
            renderGrid(g);
        }
        renderHUD(g);
    }

    private void renderQuadTree(java.awt.Graphics g) 
    {
        g.setColor(Color.RED);
        
        if ( autoScale ) {
            viewPort.autoScale();
        } else {
            viewPort.recalculateScaling();
        }
        
        tree.visitPopulatedLeafs( (x0,y0,node) -> 
        {
            tmp.setLocation( x0 , y0 );
            viewPort.modelToView( tmp );
            final int sizeW = Math.max( 1 , (int) (node.size*viewPort.scaleX) );
            final int sizeH = Math.max( 1 , (int) (node.size*viewPort.scaleY) );
            g.fillRect( tmp.x ,tmp.y , sizeW , sizeH );
        } , 1<<quadTreeLevelToRender );
    }

    private void renderHUD(java.awt.Graphics g) 
    {
        final float fps = 1f/deltaSeconds;
        final String fpsString = deltaSeconds == 0 ? "--" : Float.toString( fps ).split("\\.")[0];
        final String msg = "Generation: "+tree.generation+" , FPS: "+fpsString+", showing quad tree tevel: "+quadTreeLevelToRender+" , simulation speed: "+simulationSpeed;
        
        Rectangle2D bounds = g.getFontMetrics().getStringBounds( msg , g );
        g.setColor( getBackground() );
        g.fillRect( 0 , 0 , (int) (5+bounds.getWidth()) , (int) (15 + bounds.getHeight() ) );
        g.setColor(Color.BLACK);        
        g.drawString( msg, 5 , 15 );
    }

    private void renderGrid(java.awt.Graphics g) {
        final int w = getWidth();
        final int h = getHeight();
        
        if ( viewPort.scaleX >= 3 && viewPort.scaleY >= 3 )
        {
            g.setColor( Color.LIGHT_GRAY );
            
            int t = 0;
            int x = 0;
            while ( x < w ) 
            {
                g.drawLine( x , 0 , x , h );
                t+=1;
                x = Math.round(t*viewPort.scaleX);
            }
            
            t = 0;
            int y = 0;
            while ( y < h ) 
            {
                g.drawLine( 0  , y , w , y );
                t+=1;
                y = Math.round(t*viewPort.scaleY);
            }            
        }
    }
    
    public void toggleAutoScale()
    {
        this.autoScale = ! this.autoScale;
        if ( this.autoScale ) {
            viewPort.autoScale();
        }
        repaintNeeded = true;
    }
    
    public VisibleArea getViewPort() {
        return viewPort;
    }

    public void setCell(int x,int y) {
        this.tree.set(x, y);
        repaintNeeded = true;
    }
    
    public void toggleRunSimulation() {
        this.advanceSimulation = ! this.advanceSimulation;;
    }
    
    public void incQuadTreeLevel() {
        quadTreeLevelToRender++;
        repaintNeeded = true;
    }
    
    public void decQuadTreeLevel() 
    {
        if ( quadTreeLevelToRender > 0 ) {
            quadTreeLevelToRender--;
            repaintNeeded = true;
        }
    }
    
    public void incSimulationSpeed() {
        this.simulationSpeed++;
        if ( ! advanceSimulation ) {
            this.repaintNeeded = true;
        }        
    }
    
    public void decSimulationSpeed() 
    {
        if ( this.simulationSpeed > 1 ) {
            this.simulationSpeed--;
            if ( ! advanceSimulation ) {
                this.repaintNeeded = true;
            }
        }
    }
    
    public void toggleGrid() {
        this.renderGrid = ! renderGrid;
        this.repaintNeeded = true;
    }
    
    public boolean isRenderGrid() {
        return renderGrid;
    }
    
    public boolean isAdvanceSimulation() {
        return advanceSimulation;
    }
    
    public boolean isAutoScale() {
        return autoScale;
    }
    
    public void clear() {
        tree.clear();
        repaintNeeded = true;
    }
    
    public void reset()
    {
        clear();
        
        tree.clear();
        tree.set( 25 , 25 );
        tree.set( 26 , 25 );
        tree.set( 29 , 25 );
        tree.set( 30 , 25 );
        tree.set( 31 , 25 );
        tree.set( 28 , 24 );
        tree.set( 26 , 23 );
        
        viewPort.set(18,10,20,30);        
    }
}