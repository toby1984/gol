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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main extends JFrame 
{
    private final Duration TIMER_SPEED_MILLIS = Duration.ofMillis( 250 );
    
    private final MyPanel panel = new MyPanel();
    private final UserInputHandler userInputHandler = new UserInputHandler( panel );
    
    private final Timer timer;
    
    private boolean buttonsListenersEnabled = true;
    
    private final List<Runnable> statefulButtons = new ArrayList<>();
    
    public Main() 
    {
        super("Game Of Life");
        
        final ActionListener listener = new ActionListener() 
        {
            private long lastTick;
            
            @Override
            public void actionPerformed(ActionEvent ev) 
            {
                final long now = System.currentTimeMillis();
                long deltaMillis = lastTick == 0 ? 0 : now - lastTick;
                final float deltaSeconds = deltaMillis/1000f;
                this.lastTick = now;
                
                final boolean inputProcessed = userInputHandler.tick();
                if ( inputProcessed ) {
                    statefulButtons.forEach( r -> r.run() );
                }
                if ( panel.tick( deltaSeconds ) ) {
                    panel.repaint();
                    Toolkit.getDefaultToolkit().sync();
                }
            }
        };
        timer = new Timer( (int) TIMER_SPEED_MILLIS.toMillis() , listener);        
        timer.setInitialDelay( 0 );
        timer.setDelay( 16 );
    }
    
    public void run() 
    {
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( createToolBar() , BorderLayout.NORTH);
        getContentPane().add( panel , BorderLayout.CENTER );
        
        
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setSize( new Dimension(640,480 ) );
        setLocationRelativeTo(null);
        setVisible( true );
        
        timer.start();
        panel.requestFocus();
    }
    
    private JToolBar createToolBar() 
    {
        final JToolBar result = new JToolBar(JToolBar.HORIZONTAL);
        
        disableFocus( result );  
        
        result.add( toggleButton("Grid" , UserInputHandler.TriggerType.TOGGLE_GRID  , () -> panel.isRenderGrid() ) );
        result.add( toggleButton("AutoScale" , UserInputHandler.TriggerType.TOGGLE_AUTOSCALE , () -> panel.isAutoScale() ) );
        result.add( toggleButton("Run" , UserInputHandler.TriggerType.TOGGLE_RUN_SIMULATION , () -> panel.isAdvanceSimulation() ) );
        result.add( button("Dec speed" , UserInputHandler.TriggerType.DEC_SIMULATION_SPEED) );
        result.add( button("Inc speed" , UserInputHandler.TriggerType.INC_SIMULATION_SPEED) );
        result.add( button("Reset" , UserInputHandler.TriggerType.RESET) );
        result.add( button("Clear" , UserInputHandler.TriggerType.CLEAR) );
        result.add( button("Zoom In" , UserInputHandler.TriggerType.ZOOM_IN) );
        result.add( button("Zoom Out" , UserInputHandler.TriggerType.ZOOM_OUT) );
        
        final JButton helpButton = new JButton("Help");
        helpButton.addActionListener( ev -> 
        {
            final JDialog dialog = new JDialog();
            dialog.setModal( true );
            
            final JTextArea textArea = new JTextArea();
            textArea.setEditable( false );
            textArea.setFont( new Font(Font.MONOSPACED,Font.PLAIN , textArea.getFont().getSize() ) );
            
            
            String msg = "";
            msg += "(C) 2016 Tobias Gierke\n\n";
            msg += "w/a/s/d     -> pan viewport\n";
            msg += "q/e         -> dec/inc simulation speed\n";
            msg += "g           -> toggle grid\n";
            msg += "r           -> reset\n";
            msg += "BACKSPACE   -> Clear\n";
            msg += "ENTER       -> toggle auto-scale\n";
            msg += "MOUSE-WHEEL -> zoom in/out\n";
            msg += "+/-         -> inc/dec level of quadtree to render";
            msg += "\n\n";
            
            textArea.setColumns( 50 );
            textArea.setText( msg );

            final JPanel subPanel = new JPanel();
            subPanel.setLayout( new BorderLayout() );
            subPanel.add( textArea ,BorderLayout.CENTER );
            
            final JButton close = new JButton("Close");
            close.addActionListener( ev2 -> dialog.dispose() );
            subPanel.add( close , BorderLayout.SOUTH);
            
            dialog.getContentPane().add( subPanel );
            dialog.pack();
            dialog.setLocationRelativeTo( null );
            dialog.setVisible( true );
        });
        result.add( disableFocus( helpButton ) );
        return result;
    }
    
    private static <T extends JComponent> T disableFocus(T comp) {
        comp.setFocusable( false );
        comp.setRequestFocusEnabled( false );
        return comp;
    }
    
    private JToggleButton toggleButton(String label,UserInputHandler.TriggerType trigger,Supplier<Boolean> stateSupplier) 
    {
        final JToggleButton button = new JToggleButton(label);
        button.setSelected( stateSupplier.get() );
        button.addActionListener( ev -> 
        {
            if ( buttonsListenersEnabled ) 
            {
                userInputHandler.triggerStarted( trigger , ev.getWhen() );
                userInputHandler.triggerStopped( trigger );
            }
        });
        statefulButtons.add( () -> 
        {
            final Boolean newState = stateSupplier.get();
            if ( newState != button.isSelected() ) {
                button.setSelected( newState );
            }
        });
        return disableFocus( button );        
    }
    
    private JButton button(String label,UserInputHandler.TriggerType trigger) 
    {
        final JButton button = new JButton(label);
        
        if ( trigger.supportsRepeat ) 
        {
            button.addMouseListener( new MouseAdapter() 
            {
                @Override
                public void mousePressed(MouseEvent e) 
                {
                    if ( SwingUtilities.isLeftMouseButton( e ) ) {
                        userInputHandler.triggerStarted( trigger , e.getWhen() );
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if ( SwingUtilities.isLeftMouseButton( e ) ) {
                        userInputHandler.triggerStopped( trigger );
                    }
                }
            });
        } else {
            button.addActionListener( ev -> 
            {
                if ( buttonsListenersEnabled ) {
                    userInputHandler.triggerStarted( trigger , ev.getWhen() );
                    userInputHandler.triggerStopped( trigger );
                }
            });
        }
        return disableFocus( button );  
    }
    
    public static void main(String[] args) throws InvocationTargetException, InterruptedException 
    {
        SwingUtilities.invokeAndWait( () -> new Main().run() );
    }    
}