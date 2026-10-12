/*
 * Copyright (C) 2014-2015 José Luis Risco Martín <jlrisco@ucm.es> and 
 * Saurabh Mittal <smittal@duniptech.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * http://www.gnu.org/licenses/
 *
 * Contributors:
 *  - José Luis Risco Martín
 */
package xdevs.core.modeling;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class for the coupled models in the DEVS formalism.
 * 
 * A coupled model is a model that contains other models, and it is used to
 * create hierarchical models. The coupled model can contain atomic models and
 * other coupled models. The coupled model is used to create the structure of
 * the DEVS model.
 */
public class Coupled extends Component {

    private static final Logger LOGGER = Logger.getLogger(Coupled.class.getName());

    /**
     * The components of the coupled model.
     */
    protected LinkedList<Component> components = new LinkedList<>();
    /**
     * The input couplings of the coupled model.
     */
    protected LinkedList<Coupling<?>> ic = new LinkedList<>();
    /**
     * The external input couplings of the coupled model.
     */
    protected LinkedList<Coupling<?>> eic = new LinkedList<>();
    /**
     * The external output couplings of the coupled model.
     */
    protected LinkedList<Coupling<?>> eoc = new LinkedList<>();

    /**
     * The constructor of the coupled model.
     * @param name The name of the coupled model.
     */
    public Coupled(String name) {
        super(name);
    }

    /**
     * The constructor of the coupled model.
     */
    public Coupled() {
        this(Coupled.class.getSimpleName());
    }

    /**
     * The constructor of the coupled model.
     * @param xmlCoupled The XML element that contains the information of the
     *                   coupled model.
     */
    public Coupled(Element xmlCoupled) {
        this(xmlCoupled.getAttribute("name"));
        this.addComponentsAndCouplings(xmlCoupled);
    }

    /**
     * This method is called by the simulator to initialize the DEVS component.
     */
    @Override
    public void initialize() {
    }

    /**
     * This method is called by the simulator to check possible structural changes.
     * @return true if the model has changed its structure, false otherwise
     */
    public boolean structuralTransition() {
        return false;
    }

    /**
     * Whether this coupled model, or any nested coupled model, defines a structural
     * transition function (i.e., overrides {@link #structuralTransition()}).
     * @return true if the model has dynamic structure
     */
    public boolean isDynamic() {
        try {
            if (this.getClass().getMethod("structuralTransition").getDeclaringClass() != Coupled.class) {
                return true;
            }
        } catch (NoSuchMethodException e) {
            return false;
        }
        for (Component component : components) {
            if (component instanceof Coupled && ((Coupled) component).isDynamic()) {
                return true;
            }
        }
        return false;
    }


    /**
     * This method is called by the simulator right after the simulation ends.
     */
    @Override
    public void exit() {
    }

    /**
     * Returns the parent of the DEVS component.
     */
    @Override
    public Component getParent() {
        return parent;
    }

    /**
     * Sets the parent of the DEVS component.
     * @param parent The parent of the DEVS component.
     */
    @Override
    public void setParent(Component parent) {
        this.parent = parent;
    }

    /**
     * This method add a connection to the DEVS component.
     *
     * @param cFrom      Component at the beginning of the connection
     * @param oPortIndex Index of the source port in cFrom, starting at 0
     * @param cTo        Component at the end of the connection
     * @param iPortIndex Index of the destination port in cTo, starting at 0
     */
    @SuppressWarnings({ "unchecked" })
    public void addCoupling(Component cFrom, int oPortIndex, Component cTo, int iPortIndex) {
        if (cFrom == this) { // EIC
            Port<?> portFrom = cFrom.inPorts.get(oPortIndex);
            Port<?> portTo = cTo.inPorts.get(iPortIndex);
            Coupling<?> coupling = new Coupling(portFrom, portTo);
            eic.add(coupling);
        } else if (cTo == this) { // EOC
            Port<?> portFrom = cFrom.outPorts.get(oPortIndex);
            Port<?> portTo = cTo.outPorts.get(iPortIndex);
            Coupling<?> coupling = new Coupling(portFrom, portTo);
            eoc.add(coupling);
        } else { // IC
            Port<?> portFrom = cFrom.outPorts.get(oPortIndex);
            Port<?> portTo = cTo.inPorts.get(iPortIndex);
            Coupling<?> coupling = new Coupling(portFrom, portTo);
            ic.add(coupling);
        }
    }

    /**
     * This method adds a input port to the DEVS component.
     */
    @Override
    public void addInPort(Port<?> port) {
        super.addInPort(port);
    }

    /**
     * This method adds a output port to the DEVS component.
     */
    @Override
    public void addOutPort(Port<?> port) {
        super.addOutPort(port);
    }

    /**
     * @deprecated This method add a connection to the DEVS component. This method
     *             is deprecated because since the addition of the
     *             <code>parent</code> attribute, both components <code>cFrom</code>
     *             and <code>cTo</code> are no longer needed inside the Coupling
     *             class.
     * @param cFrom Component at the beginning of the connection
     * @param pFrom Port at the beginning of the connection
     * @param cTo   Component at the end of the connection
     * @param pTo   Port at the end of the connection
     */
    public void addCoupling(Component cFrom, Port<?> pFrom, Component cTo, Port<?> pTo) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Coupling coupling = new Coupling(pFrom, pTo);
        // Add to connections
        if (cFrom == this) {
            eic.add(coupling);
        } else if (cTo == this) {
            eoc.add(coupling);
        } else {
            ic.add(coupling);
        }
    }

    /**
     * This method add a connection to the DEVS component.
     *
     * @param cFromName Name of the component at the beginning of the connection
     * @param pFromName Name of the port at the beginning of the connection
     * @param cToName   Name of the component at the end of the connection
     * @param pToName   Name of the port at the end of the connection
     */
    public final void addCoupling(String cFromName, String pFromName, String cToName, String pToName) {
        Component cFrom = this.getComponentByName(cFromName);
        Component cTo = this.getComponentByName(cToName);
        if (cFrom == null) {
            LOGGER.severe(cFromName + " does not exist");
            return;
        }
        if (cTo == null) {
            LOGGER.severe(cToName + " does not exist");
            return;
        }
        Port<?> pFrom, pTo;
        // Add to connections
        if (cFrom == this) {
            pFrom = cFrom.getInPort(pFromName);
            pTo = cTo.getInPort(pToName);
        } else if (cTo == this) {
            pFrom = cFrom.getOutPort(pFromName);
            pTo = cTo.getOutPort(pToName);
        } else {
            pFrom = cFrom.getOutPort(pFromName);
            pTo = cTo.getInPort(pToName);
        }
        if (pFrom == null) {
            LOGGER.severe(cFrom.getName() + "::" + pFromName + "->" + cTo.getName() + "::" + pToName + " --> port "
                    + pFromName + " at component " + cFrom.getName() + " does not exist");
            return;
        }
        if (pTo == null) {
            LOGGER.severe(cFrom.getName() + "::" + pFromName + "->" + cTo.getName() + "::" + pToName + " --> port "
                    + pToName + " at component " + cTo.getName() + " does not exist");
            return;
        }
        this.addCoupling(pFrom, pTo);
    }

    /**
     * This member adds a connection between ports pFrom and pTo
     *
     * @param pFrom Port at the beginning of the connection
     * @param pTo   Port at the end of the connection
     */
    public void addCoupling(Port<?> pFrom, Port<?> pTo) {
        if (pFrom.getParent() == null) {
            LOGGER.severe("Port " + pFrom.getName()
                    + " does not have a parent component. Maybe the port was not added to the component?");
            return;
        }
        if (pTo.getParent() == null) {
            LOGGER.severe("Port " + pTo.getName()
                    + " does not have a parent component. Maybe the port was not added to the component?");
            return;
        }
        Coupling coupling = new Coupling(pFrom, pTo);
        // Add to connections
        if (pFrom.getParent() == this) {
            eic.add(coupling);
        } else if (pTo.getParent() == this) {
            eoc.add(coupling);
        } else {
            ic.add(coupling);
        }
    }

    /**
     * Get the components of the coupled model.
     * @return The components of the coupled model.
     */
    public Collection<Component> getComponents() {
        return components;
    }

    /**
     * Get the first component (including this coupled model) whose name match with
     * name argument.
     *
     * @param name The name of the component to find
     * @return The component, which name is equal to the argument. If no component
     *         is found, null is returned.
     */
    public Component getComponentByName(String name) {
        if (this.name.equals(name)) {
            return this;
        }

        for (Component component : components) {
            if (component.name.equals(name)) {
                return component;
            }
        }

        return null;
    }

    /**
     * Adds a component to the coupled model.
     * @param component The component to add to the coupled model.
     */
    public final void addComponent(Component component) {
        component.setParent(this);
        components.add(component);
    }

    /**
     * Get the input couplings of the coupled model.
     * @return The input couplings of the coupled model.
     */
    public LinkedList<Coupling<?>> getIC() {
        return ic;
    }

    /**
     * Get the external input couplings of the coupled model.
     * @return The external input couplings of the coupled model.
     */
    public LinkedList<Coupling<?>> getEIC() {
        return eic;
    }

    /**
     * Get the external output couplings of the coupled model.
     * @return The external output couplings of the coupled model.
     */
    public LinkedList<Coupling<?>> getEOC() {
        return eoc;
    }

    /**
     * This method flattens the coupled model, removing all the coupled models and
     * adding their components to the parent coupled model.
     * @return this, as the coupled model after the flattening.
     */
    public Coupled flatten() {
        if (this.isDynamic()) {
            throw new IllegalStateException("Coupled model " + name
                    + " overrides structuralTransition(): dynamic structure models cannot be flattened, "
                    + "because flattening removes the coupled models that own the structural transition functions.");
        }
        List<Coupled> toFlatten = new ArrayList<>();
        for (Component component : components) {
            if (component instanceof Coupled) {
                toFlatten.add((Coupled) component);
            }
        }
        for (Coupled coupled : toFlatten) {
            coupled.flatten();
            removePortsAndCouplings(coupled);
            components.remove(coupled);
        }
        toFlatten.clear();

        if (parent == null) {
            return this;
        }

        // Process if parent ...
        // First, we store all the parent ports connected to input ports
        HashMap<Port<?>, LinkedList<Port<?>>> leftBridgeEIC = createLeftBrige(((Coupled) parent).getEIC());
        HashMap<Port<?>, LinkedList<Port<?>>> leftBridgeIC = createLeftBrige(((Coupled) parent).getIC());
        // The same with the output ports
        HashMap<Port<?>, LinkedList<Port<?>>> rightBridgeEOC = createRightBrige(((Coupled) parent).getEOC());
        HashMap<Port<?>, LinkedList<Port<?>>> rightBridgeIC = createRightBrige(((Coupled) parent).getIC());

        completeLeftBridge(eic, leftBridgeEIC, ((Coupled) parent).getEIC());
        completeLeftBridge(eic, leftBridgeIC, ((Coupled) parent).getIC());
        completeRightBridge(eoc, rightBridgeEOC, ((Coupled) parent).getEOC());
        completeRightBridge(eoc, rightBridgeIC, ((Coupled) parent).getIC());

        components.forEach((component) -> {
            ((Coupled) parent).addComponent(component);
        });

        ic.forEach((cIC) -> {
            ((Coupled) parent).getIC().add(cIC);
        });
        return this;
    }

    /**
     * Auxiliary method for the flatten method.
     * @param couplings The couplings to process
     * @param leftBridge The left bridge to complete
     * @param pCouplings The couplings of the parent
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void completeLeftBridge(LinkedList<Coupling<?>> couplings, HashMap<Port<?>, LinkedList<Port<?>>> leftBridge,
            LinkedList<Coupling<?>> pCouplings) {
        for (Coupling<?> c : couplings) {
            LinkedList<Port<?>> list = leftBridge.get(c.portFrom);
            if (list != null) {
                for (Port<?> port : list) {
                    pCouplings.add(new Coupling(port, c.portTo));
                }
            }
        }
    }

    /**
     * Auxiliary method for the flatten method.
     * @param couplings The couplings to process
     * @param rightBridge The right bridge to complete
     * @param pCouplings The couplings of the parent
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void completeRightBridge(LinkedList<Coupling<?>> couplings,
            HashMap<Port<?>, LinkedList<Port<?>>> rightBridge, LinkedList<Coupling<?>> pCouplings) {
        for (Coupling<?> c : couplings) {
            LinkedList<Port<?>> list = rightBridge.get(c.portTo);
            if (list != null) {
                for (Port<?> port : list) {
                    pCouplings.add(new Coupling(c.portFrom, port));
                }
            }
        }
    }

    /**
     * Auxiliary method for the flatten method.
     * @param couplings The couplings to process
     * @return The left bridge
     */
    private HashMap<Port<?>, LinkedList<Port<?>>> createLeftBrige(LinkedList<Coupling<?>> couplings) {
        HashMap<Port<?>, LinkedList<Port<?>>> leftBridge = new HashMap<>();
        for (Port<?> iPort : this.inPorts) {
            for (Coupling<?> c : couplings) {
                if (c.portTo == iPort) {
                    LinkedList<Port<?>> list = leftBridge.get(iPort);
                    if (list == null) {
                        list = new LinkedList<>();
                        leftBridge.put(iPort, list);
                    }
                    list.add(c.portFrom);
                }
            }
        }
        return leftBridge;
    }

    /**
     * Auxiliary method for the flatten method.
     * @param couplings The couplings to process
     * @return The right bridge
     */
    private HashMap<Port<?>, LinkedList<Port<?>>> createRightBrige(LinkedList<Coupling<?>> couplings) {
        HashMap<Port<?>, LinkedList<Port<?>>> rightBridge = new HashMap<>();
        for (Port<?> oPort : this.outPorts) {
            for (Coupling<?> c : couplings) {
                if (c.portFrom == oPort) {
                    LinkedList<Port<?>> list = rightBridge.get(oPort);
                    if (list == null) {
                        list = new LinkedList<>();
                        rightBridge.put(oPort, list);
                    }
                    list.add(c.portTo);
                }
            }
        }
        return rightBridge;
    }

    /**
     * Remove a component, and related coupling relations from the coupled model.
     * @param child The component to remove
     */
    protected void removeComponent(Component child) {
        this.removePortsAndCouplings(child);
        this.components.remove(child);
    }

    /**
     * Remove the ports and couplings related to a component.
     * @param child The component to remove
     */
    private void removePortsAndCouplings(Component child) {
        Collection<Port<?>> inPorts = child.getInPorts();
        for (Port<?> iport : inPorts) {
            for (int j = 0; j < eic.size(); ++j) {
                Coupling<?> c = eic.get(j);
                if (c.portTo == iport) {
                    eic.remove(j--);
                }
            }
            for (int j = 0; j < ic.size(); ++j) {
                Coupling<?> c = ic.get(j);
                if (c.portTo == iport) {
                    ic.remove(j--);
                }
            }
        }
        Collection<Port<?>> outPorts = child.getOutPorts();
        for (Port<?> oport : outPorts) {
            for (int j = 0; j < eoc.size(); ++j) {
                Coupling<?> c = eoc.get(j);
                if (c.portFrom == oport) {
                    eoc.remove(j--);
                }
            }
            for (int j = 0; j < ic.size(); ++j) {
                Coupling<?> c = ic.get(j);
                if (c.portFrom == oport) {
                    ic.remove(j--);
                }
            }
        }
    }

    /**
     * This method returns the XML representation of the coupled model.
     * @return The XML representation of the coupled model.
     */
    public String toXml() {
        StringBuilder builder = new StringBuilder();
        StringBuilder tabs = new StringBuilder();
        Component parent = this.getParent();
        int level = 0;
        while (parent!=null) {
            tabs.append("\t");
            parent = parent.parent;
            level++;
        }

        if (level == 0) {
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        }
        builder.append(tabs).append("<coupled name=\"" + this.getName() + "\"");
        builder.append(" class=\"").append(this.getClass().getCanonicalName()).append("\"");
        builder.append(" host=\"127.0.0.1\"");
        builder.append(" port=\"").append(5000 + level).append("\"");
        builder.append(">\n");
        for (Component component : components) {
            builder.append(component.toXml());
        }
        // Couplings
        LinkedList<Coupling<?>> allCouplings = getEIC();
        allCouplings.addAll(getIC());
        allCouplings.addAll(getEOC());
        allCouplings.forEach((coupling) -> {
            builder.append(tabs).append("\t<connection");
            builder.append(" componentFrom=\"").append(coupling.getPortFrom().getParent().getName()).append("\"");
            builder.append(" classFrom=\"").append(coupling.getPortFrom().getParent().getClass().getCanonicalName())
                    .append("\"");
            builder.append(" portFrom=\"").append(coupling.getPortFrom().getName()).append("\"");
            builder.append(" componentTo=\"").append(coupling.getPortTo().getParent().getName()).append("\"");
            builder.append(" classTo=\"").append(coupling.getPortTo().getParent().getClass().getCanonicalName())
                    .append("\"");
            builder.append(" portTo=\"").append(coupling.getPortTo().getName()).append("\"");
            builder.append("/>\n");
        });
        builder.append(tabs).append("</coupled>\n");
        return builder.toString();
    }

    /**
     * This method returns the number of atomic components in the coupled model.
     * @return The number of atomic components in the coupled model.
     */
    public int countAtomicComponents() {
        int res = 0;
        for (Component component : components) {
            if (component instanceof Atomic) {
                res++;
            } else {
                res += ((Coupled) component).countAtomicComponents();
            }
        }
        return res;
    }

    /**
     * Given a XML representation of a coupled model, this method adds the
     * components and couplings to the current coupled model.
     * @param xmlCoupled The XML representation of the coupled model.
     */
    protected void addComponentsAndCouplings(Element xmlCoupled) {
        // Creamos los distintos elementos
        NodeList xmlChildList = xmlCoupled.getChildNodes();
        for (int i = 0; i < xmlChildList.getLength(); ++i) {
            Node xmlNode = xmlChildList.item(i);
            Element xmlChild;
            String nodeName = xmlNode.getNodeName();
            switch (nodeName) {
            case "coupled":
                xmlChild = (Element) xmlNode;
                try {
                    Class<?> coupledClass = Class.forName(xmlChild.getAttribute("class"));
                    Constructor<?> constructor = coupledClass
                            .getConstructor(new Class[] { String.class });
                    Object coupledObject = constructor.newInstance(new Object[] { xmlChild.getAttribute("name") });
                    Coupled coupledChild = (Coupled)coupledObject;
                    this.addComponent(coupledChild);
                    coupledChild.addComponentsAndCouplings(xmlChild);
                } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
                        | InstantiationException | NoSuchMethodException | SecurityException
                        | InvocationTargetException ex) {
                    LOGGER.severe(ex.getLocalizedMessage());
                }
                break;
            case "atomic":
                xmlChild = (Element) xmlNode;
                try {
                    Class<?> atomicClass = Class.forName(xmlChild.getAttribute("class"));
                    Constructor<?> constructor = atomicClass
                            .getConstructor(new Class[] { Class.forName("org.w3c.dom.Element") });
                    Object atomicObject = constructor.newInstance(new Object[] { xmlChild });
                    Atomic atomicChild = (Atomic)atomicObject;
                    this.addComponent(atomicChild);
                } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
                        | InstantiationException | NoSuchMethodException | SecurityException
                        | InvocationTargetException ex) {
                    LOGGER.severe(ex.getLocalizedMessage());
                }
                break;
            case "connection":
                xmlChild = (Element) xmlNode;
                String componentFrom = xmlChild.getAttribute("componentFrom");
                String portFrom = xmlChild.getAttribute("portFrom");
                String componentTo = xmlChild.getAttribute("componentTo");
                String portTo = xmlChild.getAttribute("portTo");
                this.addCoupling(componentFrom, portFrom, componentTo, portTo);
                break;
            default:
                break;
            }
        }

    }
}
