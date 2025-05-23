package org.atalk.util.event;

public abstract interface ContainerListener {
    void componentAdded(ContainerEvent paramContainerEvent);

    void componentRemoved(ContainerEvent paramContainerEvent);
}
