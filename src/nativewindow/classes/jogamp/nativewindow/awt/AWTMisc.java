/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.nativewindow.awt;

import java.awt.FocusTraversalPolicy;
import java.awt.Insets;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.WindowConstants;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.swing.MenuSelectionManager;

public class AWTMisc {

    public static JFrame getJFrame(Component c) {
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (JFrame) c;
    }

    public static Frame getFrame(Component c) {
        while (c != null && !(c instanceof Frame)) {
            c = c.getParent();
        }
        return (Frame) c;
    }

    public static Window getWindow(Component c) {
        while (c != null && !(c instanceof Window)) {
            c = c.getParent();
        }
        return (Window) c;
    }

    public static Container getContainer(Component c) {
        while (c != null && !(c instanceof Container)) {
            c = c.getParent();
        }
        return (Container) c;
    }

    /**
     * Return insets of the component w/o traversing up to parent,
     * i.e. trying Window and JComponent.
     * <p>
     * Exception is JRootPane.
     * Return it's parent's Window component's insets if available,
     * otherwise return JRootPane's insets.<br>
     * This is due to <i>experience</i> that <i>some</i> JRootPane's
     * do not expose valid insets value.
     * </p>
     * @param topLevelOnly if true only returns insets of top-level components, i.e. Window and JRootPanel,
     * otherwise for JComponent as well.
     */
    public static Insets getInsets(Component c, boolean topLevelOnly) {
        if( c instanceof Window ) {
            return ((Window)c).getInsets();
        }
        if( c instanceof JRootPane ) {
            final Window w = getWindow(c);
            if( null != w ) {
                return w.getInsets();
            }
            return ((JRootPane)c).getInsets();
        }
        if( !topLevelOnly && c instanceof JComponent ) {
            return ((JComponent)c).getInsets();
        }
        return null;
    }

    public static interface ComponentAction {
        /**
         * @param c the component to perform the action on
         */
        public void run(Component c);
    }

    public static int performAction(Container c, Class<?> cType, ComponentAction action) {
        int count = 0;
        final int cc = c.getComponentCount();
        for(int i=0; i<cc; i++) {
            final Component e = c.getComponent(i);
            if( e instanceof Container ) {
                count += performAction((Container)e, cType, action);
            } else if( cType.isInstance(e) ) {
                action.run(e);
                count++;
            }
        }
        // we come at last ..
        if( cType.isInstance(c) ) {
            action.run(c);
            count++;
        }
        return count;
    }

    /**
     * Traverse to the next forward or backward component using the
     * container's FocusTraversalPolicy.
     *
     * @param comp the assumed current focuse component
     * @param forward if true, returns the next focus component, otherwise the previous one.
     * @return
     */
    public static Component getNextFocus(Component comp, boolean forward) {
        Container focusContainer = comp.getFocusCycleRootAncestor();
        while ( focusContainer != null &&
                ( !focusContainer.isShowing() || !focusContainer.isFocusable() || !focusContainer.isEnabled() ) )
        {
            comp = focusContainer;
            focusContainer = comp.getFocusCycleRootAncestor();
        }
        Component next = null;
        if (focusContainer != null) {
            final FocusTraversalPolicy policy = focusContainer.getFocusTraversalPolicy();
            next = forward ? policy.getComponentAfter(focusContainer, comp) : policy.getComponentBefore(focusContainer, comp);
            if (next == null) {
                next = policy.getDefaultComponent(focusContainer);
            }
        }
        return next;
    }

    /**
     * Issue this when your non AWT toolkit gains focus to clear AWT menu path
     */
    public static void clearAWTMenus() {
        MenuSelectionManager.defaultManager().clearSelectedPath();
    }

    public static WindowClosingProtocol.WindowClosingMode AWT2NWClosingOperation(int awtClosingOperation) {
        switch (awtClosingOperation) {
            case WindowConstants.DISPOSE_ON_CLOSE:
            case WindowConstants.EXIT_ON_CLOSE:
                return WindowClosingProtocol.WindowClosingMode.DISPOSE_ON_CLOSE;
            case WindowConstants.DO_NOTHING_ON_CLOSE:
            case WindowConstants.HIDE_ON_CLOSE:
                return WindowClosingProtocol.WindowClosingMode.DO_NOTHING_ON_CLOSE;
            default:
                throw new NativeWindowException("Unhandled AWT Closing Operation: " + awtClosingOperation);
        }
    }

    public static WindowClosingProtocol.WindowClosingMode getNWClosingOperation(Component c) {
        final JFrame jf = getJFrame(c);
        final int op = (null != jf) ? jf.getDefaultCloseOperation() : WindowConstants.DO_NOTHING_ON_CLOSE ;
        return AWT2NWClosingOperation(op);
    }
}
