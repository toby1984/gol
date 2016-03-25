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

import java.util.HashMap;
import java.util.Map;

public class QuadTree 
{
    public int xTopLeft; // x coordinate of top-left corner
    public int yTopLeft; // y coordinate of top-left corner
    
    public Node root;
    
    public int generation;
    
    private static Map<Long,Integer> neighbourCounts = new HashMap<>();
    
    protected static final class Point 
    {
        public int x,y;
        
        public Point(int x,int y) 
        {
            this.x = x;
            this.y = y;
        }
        
        public void set(int x,int y) {
            this.x = x;
            this.y = y;
        }
        
        public Point add(int dx,int dy) {
            this.x += dx;
            this.y += dy;
            return this;
        }
        
        @Override
        public String toString() {
            return "("+x+","+y+")";
        }
        
        public Point nw(int size) {
            return this;
        }
        
        public Point ne(int size) {
            this.x += size;
            return this;
        }  
        
        public Point sw(int size) {
            this.y += size;
            return this;
        } 
        
        public Point se(int size) {
            this.x += size;
            this.y += size;
            return this;
        }         
    }
    
    protected static final class Node 
    {
        public Node nw;
        public Node ne;
        public Node se;
        public Node sw;

        public final int size; // node covers size*size cells
        
        public Node(int size) 
        {
            if ( size < 1) {
                throw new IllegalArgumentException("Size must be >= 1 , was: "+size);
            }            
            this.size = size;
        }    
        
        public Node(int size,Node nw,Node ne,Node se,Node sw) 
        {
            if ( size < 1) {
                throw new IllegalArgumentException("Size must be >= 1 , was: "+size);
            }            
            this.size = size;
            this.nw = nw;
            this.ne = ne;
            this.sw = sw;
            this.se = se;
        }         
        
        public boolean isSet() 
        {
            if ( size == 1 ) {
                return true;
            }
            return (nw != null && nw.isSet() ) || (ne != null && ne.isSet()) || (sw != null && sw.isSet()) || (se != null && se.isSet());
        }
        
        public boolean isSet(int xTopLeft,int yTopLeft,int x,int y) 
        {
            if ( size == 1 ) {
                return true;
            }
            final int nextLevel = this.size/2;
            if ( ne != null && neContains( xTopLeft, yTopLeft , x , y ) ) 
            {
                return ne.isSet( xTopLeft+nextLevel , yTopLeft , x,y);
            }
            if ( nw != null && nwContains( xTopLeft, yTopLeft, x , y ) ) {
                return nw.isSet( xTopLeft , yTopLeft , x,y);
            }
            if ( se != null && seContains( xTopLeft, yTopLeft, x , y ) ) {
                return se.isSet( xTopLeft+nextLevel , yTopLeft+nextLevel ,  x,y);
            }
            if ( sw != null && swContains( xTopLeft, yTopLeft, x , y ) ) {
                return sw.isSet( xTopLeft , yTopLeft+nextLevel , x,y);
            }
            return false;
        }
        
        public void set(Point topLeft, int x,int y) 
        {
            if ( size == 1 ) 
            { 
                return;
            }
            final int nextSize = this.size/2;
            if ( nwContains(topLeft.x,topLeft.y,x,y) ) 
            {
                if ( nw == null ) 
                {
                    nw = new Node( this.size/2);
                }
                nw.set(topLeft.nw( nextSize ) ,x,y);
            } 
            else if ( neContains( topLeft.x,topLeft.y,x ,  y ) ) 
            {
                if ( ne == null ) {
                    ne = new Node( this.size/2);
                }
                ne.set(topLeft.ne( nextSize ),x, y);
            } 
            else if ( swContains( topLeft.x,topLeft.y,x , y ) ) {
                if ( sw == null ) {
                    sw = new Node( this.size/2);
                }                
                sw.set(topLeft.sw( nextSize ) ,x,y);
            }
            else if ( seContains(topLeft.x,topLeft.y, x , y ) ) 
            {
                if ( se == null ) {
                    se = new Node( this.size/2);
                }                
                se.set(topLeft.se( nextSize ) ,x, y);
            } else {
                throw new RuntimeException("Unreachable code reached: "+topLeft+",size="+size+" cannot contain ("+x+","+y+")");
            }
        }
       
        private boolean nwContains(int xTopLeft,int yTopLeft,int x,int y) 
        {
            final int x1 = xTopLeft + size/2;
            final int y1 = yTopLeft + size/2;
            return xTopLeft  <= x && x < x1 &&
                    yTopLeft <= y && y < y1;
        }
        
        private boolean neContains(int xTopLeft,int yTopLeft,int x,int y) 
        {
            final int px0 = xTopLeft + size/2;
            final int py0 = yTopLeft;
            final int px1 = px0 + size/2;
            final int py1 = py0 + size/2;
            
            return  px0 <= x && x < px1 &&
                    py0 <= y && y < py1;
        }
        
        private boolean seContains(int xTopLeft,int yTopLeft,int x,int y) 
        {
            final int px0 = xTopLeft + size/2;
            final int py0 = yTopLeft + size/2;
            final int px1 = px0 + size/2;
            final int py1 = py0 + size/2;
            
            return  px0 <= x && x < px1 &&
                    py0 <= y && y < py1;
        }     
        
        private boolean swContains(int xTopLeft,int yTopLeft,int x,int y) 
        {
            final int px0 = xTopLeft;
            final int py0 = yTopLeft + size/2;
            final int px1 = px0 + size/2;
            final int py1 = py0 + size/2;
            
            return  px0 <= x && x < px1 &&
                    py0 <= y && y < py1;
        }  
        
        public void visitPopulatedLeafs( int xTopLeft,int yTopLeft,INodeVisitor visitor,int sizeToVisit) 
        {
            if ( this.size == sizeToVisit ) 
            {
                if ( isSet() ) 
                {
                    visitor.visit( xTopLeft , yTopLeft , this );
                }
                return;
            } 
            if ( this.size < sizeToVisit ) {
                return;
            }
            
            final int nextSize = this.size/2;
            if ( nw != null ) {
                nw.visitPopulatedLeafs( xTopLeft , yTopLeft , visitor,sizeToVisit );
            }
            if ( ne != null ) {
                ne.visitPopulatedLeafs( xTopLeft+nextSize , yTopLeft , visitor,sizeToVisit );
            }
            if ( sw != null ) {
                sw.visitPopulatedLeafs( xTopLeft , yTopLeft + nextSize , visitor,sizeToVisit );
            }
            if ( se != null ) {
                se.visitPopulatedLeafs( xTopLeft+nextSize,yTopLeft+nextSize , visitor,sizeToVisit );
            }
        }         
    }
    
    public int getLivingNeighbourCount(int x,int y) 
    {
        final Long key = ((long) x & 0xffffffff) << 32 | ((long) y & 0xffffffff);
        final Integer existing = neighbourCounts.get( key );
        if ( existing != null ) {
            return existing.intValue();
        }
        
        // nw
        int count = 0;
        if ( contains( x-1 , y-1 ) && root.isSet( xTopLeft,yTopLeft , x-1 , y-1 ) ) {
            count++;
        }
        
        // n
        if ( contains( x , y-1 ) && root.isSet( xTopLeft,yTopLeft , x , y-1 ) ) {
            count++;
        }    
        
        // ne
        if ( contains( x+1 , y-1 ) && root.isSet( xTopLeft,yTopLeft , x+1 , y-1 ) ) {
            count++;
        }            
        
        // e
        if ( contains( x+1 , y ) && root.isSet( xTopLeft,yTopLeft , x+1 , y ) ) {
            count++;
        }           
        
        // se
        if ( contains( x+1 , y+1 ) && root.isSet( xTopLeft,yTopLeft , x+1 , y+1 ) ) {
            count++;
        }           
        
        // s
        if ( contains( x , y+1 ) && root.isSet( xTopLeft,yTopLeft , x , y+1 ) ) {
            count++;
        }          
        
        // sw
        if ( contains( x-1 , y+1 ) && root.isSet( xTopLeft,yTopLeft , x-1 , y+1 ) ) {
            count++;
        }         
        
        // w
        if ( contains( x-1 , y ) && root.isSet( xTopLeft,yTopLeft , x-1 , y ) ) {
            count++;
        }         
        neighbourCounts.put( key , count );
        return count;
    }
    
    public void set(int x,int y) 
    {
        if ( root == null ) 
        {
            xTopLeft = x;
            yTopLeft = y;
            root = new Node(1);
            return;
        }
        
        while ( ! contains( x, y ) ) 
        {
            if ( x <= xTopLeft )
            {
                if ( y <= yTopLeft ) 
                {
                    xTopLeft = xTopLeft - root.size;
                    yTopLeft = yTopLeft - root.size;
                    root = new Node( root.size*2 , null , null , root , null );
                } else {
                    xTopLeft = xTopLeft - root.size;
                    root = new Node( root.size*2 , null , root , null , null  );
                }
            } 
            else // x > root.x0 
            {
                if ( y <= yTopLeft ) 
                {                
                    yTopLeft = yTopLeft - root.size;
                    root = new Node( root.size*2 , null , null , null , root  );
                } else {
                    root = new Node( root.size*2 , root , null , null , null );
                }
            }
        }
        root.set( new Point( xTopLeft , yTopLeft ) , x,y);
        return;        
    }
    
    public interface INodeVisitor 
    {
        public void visit(int x0,int y0,Node node);
    }
    
    public void visitPopulatedLeafs(INodeVisitor visitor,int size) 
    {
        if ( size < 1 ) {
            throw new IllegalArgumentException("Invalid size");
        }
        if ( root !=null ) 
        {
            root.visitPopulatedLeafs( xTopLeft , yTopLeft , visitor , size );
        }
    }    
    
    public static int log2( int value )
    {
        if( value <= 0 ) {
            throw new IllegalArgumentException("value out of valid range: "+value);
        }
        return 31 - Integer.numberOfLeadingZeros( value );
    }
    
    public void nextGeneration() 
    {
        neighbourCounts.clear();
        
        // visit life cells
        final QuadTree newTree = new QuadTree(this.generation+1);
        final INodeVisitor visitor = (x0,y0,node) -> 
        {
            if ( calcCellStatus(x0,y0 ) ) {
                newTree.set(x0, y0);
            }
            
            // north-west
            if ( ! isSet( x0-1 , y0-1 ) && getLivingNeighbourCount( x0-1 , y0-1 ) == 3 ) {
                newTree.set( x0-1 , y0-1 );
            } 
            
            // north
            if ( ! isSet( x0 , y0-1 ) && getLivingNeighbourCount( x0 , y0-1 ) == 3 ) {
                newTree.set( x0 , y0-1 );
            }
            
            // north-east
            if ( ! isSet( x0+1 , y0-1 ) && getLivingNeighbourCount( x0+1 , y0-1 ) == 3 ) {
                newTree.set( x0+1 , y0-1 );
            } 
            
            // south-east
            if ( ! isSet( x0+1 , y0+1 ) && getLivingNeighbourCount( x0+1 , y0+1 ) == 3 ) {
                newTree.set( x0+1 , y0+1 );
            }               
            
            // south
            if ( ! isSet( x0 , y0+1 ) && getLivingNeighbourCount( x0 , y0+1 ) == 3 ) {
                newTree.set( x0 , y0+1 );
            }          
            
            // south-west
            if ( ! isSet( x0-1 , y0+1 ) && getLivingNeighbourCount( x0-1 , y0+1 ) == 3 ) {
                newTree.set( x0-1 , y0+1 );
            } 
            // east
            if ( ! isSet( x0-1 , y0 ) && getLivingNeighbourCount( x0-1 , y0 ) == 3 ) {
                newTree.set( x0-1 , y0 );
            }    
            // west
            if ( ! isSet( x0+1 , y0 ) && getLivingNeighbourCount( x0+1 , y0 ) == 3 ) {
                newTree.set( x0+1 , y0 );
            }             
        };
        
        visitPopulatedLeafs( visitor , 1 );
        
        this.root = newTree.root;
        this.xTopLeft = newTree.xTopLeft;
        this.yTopLeft = newTree.yTopLeft;
        this.generation++;
    }
    
    public boolean isSet(int x,int y) 
    {
        return root != null && root.isSet( xTopLeft , yTopLeft , x , y );
    }
    
    private boolean calcCellStatus(int x0,int y0) 
    {
        switch( getLivingNeighbourCount( x0 ,  y0 ) ) {
            case 0:
            case 1:
                // Any live cell with fewer than two live neighbours dies, as if caused by under-population.
                return false;
            case 2:
            case 3:
                // Any live cell with two or three live neighbours lives on to the next generation.
                return true;
            default:
                // Any live cell with more than three live neighbours dies, as if by over-population.
                return false;
        }
    }
    
    public boolean contains(int x,int y) 
    {
        if ( root == null ) {
            return false;
        }
        return xTopLeft <= x && x < (xTopLeft+root.size) &&
               yTopLeft <= y && y < (yTopLeft+root.size);
    }
    
    public QuadTree(int generation) {
        this.generation = generation;
    }
    
    public void clear() {
        root = null;
        generation = 1;
    }
}