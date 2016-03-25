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

import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.HashMap;
import java.util.Map;

public class UserInputHandler 
{
    private static final Map<Integer,TriggerType> KEYCODE_TO_TRIGGERTYPE = new HashMap<>();

    public static enum TriggerType 
    {
        CLEAR                 (KeyEvent.VK_BACK_SPACE),
        RESET                 (KeyEvent.VK_R),
        TOGGLE_GRID           (KeyEvent.VK_G),
        INC_SIMULATION_SPEED  (KeyEvent.VK_E,true),
        DEC_SIMULATION_SPEED  (KeyEvent.VK_Q,true),
        DEC_QUADTREE_LEVEL    (KeyEvent.VK_MINUS),
        INC_QUADTREE_LEVEL    (KeyEvent.VK_PLUS),
        TOGGLE_RUN_SIMULATION (KeyEvent.VK_SPACE),
        TOGGLE_AUTOSCALE      (KeyEvent.VK_ENTER),
        ZOOM_IN               (KeyEvent.VK_COMMA,true),
        ZOOM_OUT              (KeyEvent.VK_DECIMAL,true),
        TRANSLATE_PLUS_Y      (KeyEvent.VK_S,true),
        TRANSLATE_MINUX_Y     (KeyEvent.VK_W,true),
        TRANSLATE_PLUS_X      (KeyEvent.VK_D,true),
        TRANSLATE_MINUS_X     (KeyEvent.VK_A,true);

        public final Integer keyCode;
        public final boolean supportsRepeat;

        private TriggerType(Integer keyCode) {
            this(keyCode,false);
        }
        
        private TriggerType(Integer keyCode,boolean supportsRepeat) {
            this.keyCode = keyCode;
            this.supportsRepeat = supportsRepeat;
            KEYCODE_TO_TRIGGERTYPE.put( keyCode , this );
        }

        public boolean isRepeatable() {
            return supportsRepeat;
        }
        
        public boolean isOneShot() {
            return ! isRepeatable();
        }        
        
        @Override
        public String toString() {
            return name()+" , repeatable: "+supportsRepeat;
        }
    }

    public static final int KEYPRESS_REPEAT_MILLIS = 50;
    public static final int TRANSLATE_X_AXIS = 5;
    public static final int TRANSLATE_Y_AXIS = 5;
    public static final int MOUSEWHEEL_ZOOM_FACTOR = 10;

    private MyPanel panel;

    private final Map<TriggerType,Trigger> pressedKeys = new HashMap<>();   

    protected static final class Trigger 
    {
        public final TriggerType type;
        public final long timestamp;
        public long lastPressHandledTimestamp;
        public boolean alreadyReleased;

        public Trigger(TriggerType type, long timestamp) {
            this.type = type;
            this.timestamp = timestamp;
        }

        public boolean hasBeenProcessed() {
            return ! wasNeverProcessed();
        }

        public boolean wasNeverProcessed() {
            return lastPressHandledTimestamp == 0;
        }

        public long timeSinceLastHandled(long now) 
        {
            if ( lastPressHandledTimestamp == 0 ) {
                return now - timestamp;
            }
            return now - lastPressHandledTimestamp;
        }
    }

    private final MouseAdapter mouseListener = new MouseAdapter() 
    {
        private int px = -1;
        private int py = -1;

        public void mouseDragged(java.awt.event.MouseEvent e) 
        {
            if ( px != -1 && py != -1 ) 
            {
                final int dx = e.getX() - px;
                final int dy = e.getY() - py;

                if ( dx <= 1 && dy <= 1 ) {
                    set( e.getX() , e.getY() );
                } 
                else 
                {
                    final int max = Math.max(dx, dy);
                    final float stepSize = 0.5f*( 1f/(float) (max+1) );
                    for ( float t = 0 ; t < 1 ; t += stepSize ) 
                    {
                        final int x = px + (int) (t*dx);
                        final int y = py + (int) (t*dy);
                        set( x ,y );
                    }
                }
                px = e.getX();
                py = e.getY();
            }
        }

        public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) 
        {
            final int clicksRotated  = e.getWheelRotation();
            if ( clicksRotated < 0 ) {
                viewport().zoomIn( -clicksRotated * MOUSEWHEEL_ZOOM_FACTOR );
            } else {
                viewport().zoomOut( clicksRotated * MOUSEWHEEL_ZOOM_FACTOR );
            }
        }

        private void set(int x,int y) 
        {
            final Point p = new Point(x,y);
            viewport().viewToModel( p );
            panel.setCell( p.x , p.y );
        }

        public void mousePressed(java.awt.event.MouseEvent e) 
        {
            px = e.getX();
            py = e.getY();
        }

        public void mouseReleased(java.awt.event.MouseEvent e) {
            px = py = -1;
        };
    };    

    public UserInputHandler(MyPanel panel) {
        this.panel = panel;
        panel.addKeyListener( new KeyAdapter() 
        {
            public void keyReleased(java.awt.event.KeyEvent e) 
            {
                final TriggerType type = KEYCODE_TO_TRIGGERTYPE.get( e.getKeyCode() );
                if ( type != null ) {
                    triggerStopped( type );
                }
            }

            public void keyPressed(KeyEvent e) 
            {
                final TriggerType type = KEYCODE_TO_TRIGGERTYPE.get( e.getKeyCode() );
                if ( type != null ) {
                    triggerStarted( type , e.getWhen() );
                }
            }
        });
        panel.addMouseMotionListener( mouseListener );
        panel.addMouseListener( mouseListener );
        panel.addMouseWheelListener( mouseListener );        
    }

    protected MyPanel.VisibleArea viewport() {
        return panel.getViewPort();
    }

    public void triggerStopped(TriggerType type) 
    {
        final Trigger existing = pressedKeys.get( type );
        if ( existing != null ) 
        {
            System.out.println("Released: "+type);              
            if ( existing.wasNeverProcessed() ) {
                existing.alreadyReleased=true;
            } else {
                pressedKeys.remove( type );
            }
        }      
    }    

    public void triggerStarted(TriggerType type,long when) 
    {
        final Trigger existing = pressedKeys.get( type );
        if ( existing == null || existing.alreadyReleased ) 
        {
            System.out.println("Triggered: "+type);
            pressedKeys.put( type , new Trigger( type  , when ) ); 
        }        
    }

     private boolean checkTrigger(TriggerType type,long now , Runnable consumer) 
     {
         final Trigger existing = pressedKeys.get( type );
         if ( existing != null && ( existing.wasNeverProcessed() || existing.timeSinceLastHandled( now ) > KEYPRESS_REPEAT_MILLIS ) ) 
         {
             if ( existing.alreadyReleased || ( existing.hasBeenProcessed() && type.isOneShot() ) ) 
             {
                 pressedKeys.remove( type );
                 if ( existing.hasBeenProcessed() ) {
                     return false;
                 }
             }
             existing.lastPressHandledTimestamp = now;
             consumer.run();
             return true;
         }
         return false;
     }

     public boolean tick() {

         final long now = System.currentTimeMillis();
         boolean inputProcessed = checkTrigger( TriggerType.TRANSLATE_MINUS_X , now , () -> viewport().translate( -TRANSLATE_X_AXIS , 0 ) );
         inputProcessed |= checkTrigger( TriggerType.TRANSLATE_PLUS_X , now , () -> viewport().translate( TRANSLATE_X_AXIS , 0 ) );
         inputProcessed |= checkTrigger( TriggerType.TRANSLATE_MINUX_Y , now , () -> viewport().translate( 0 , -TRANSLATE_Y_AXIS ) );
         inputProcessed |= checkTrigger( TriggerType.TRANSLATE_PLUS_Y , now , () -> viewport().translate( 0 , TRANSLATE_Y_AXIS ) );
         inputProcessed |= checkTrigger( TriggerType.TOGGLE_AUTOSCALE , now , () ->  panel.toggleAutoScale() );
         inputProcessed |= checkTrigger( TriggerType.TOGGLE_RUN_SIMULATION , now ,  () -> panel.toggleRunSimulation( ) );
         inputProcessed |= checkTrigger( TriggerType.INC_QUADTREE_LEVEL , now , () -> panel.incQuadTreeLevel() );
         inputProcessed |= checkTrigger( TriggerType.DEC_QUADTREE_LEVEL , now , () -> panel.decQuadTreeLevel() );
         inputProcessed |= checkTrigger( TriggerType.DEC_SIMULATION_SPEED , now , () -> panel.decSimulationSpeed() );
         inputProcessed |= checkTrigger( TriggerType.INC_SIMULATION_SPEED , now , () -> panel.incSimulationSpeed() );
         inputProcessed |= checkTrigger( TriggerType.TOGGLE_GRID , now , () -> panel.toggleGrid() );
         inputProcessed |= checkTrigger( TriggerType.RESET, now , () -> panel.reset() );
         inputProcessed |= checkTrigger( TriggerType.CLEAR, now , () -> panel.clear() );
         inputProcessed |= checkTrigger( TriggerType.ZOOM_IN , now , () -> viewport().zoomIn( MOUSEWHEEL_ZOOM_FACTOR ) );
         inputProcessed |= checkTrigger( TriggerType.ZOOM_OUT, now , () -> viewport().zoomOut( MOUSEWHEEL_ZOOM_FACTOR ) );
         return inputProcessed;
     } 
}