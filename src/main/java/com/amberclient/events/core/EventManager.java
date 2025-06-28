package com.amberclient.events.core;

import com.amberclient.events.network.PacketReceiveListener;
import com.amberclient.events.player.*;
import net.minecraft.network.packet.Packet;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EventManager {
    private static final EventManager INSTANCE = new EventManager();
    private final List<PreMotionListener> preMotionListeners = new ArrayList<>();
    private final List<PostMotionListener> postMotionListeners = new ArrayList<>();
    private final List<PacketReceiveListener> packetReceiveListeners = new ArrayList<>();
    private final List<PreVelocityListener> preVelocityListeners = new ArrayList<>();
    private final List<PostVelocityListener> postVelocityListeners = new ArrayList<>();

    private final List<Object> registeredObjects = new ArrayList<>();

    public EventManager() {
    }

    public static EventManager getInstance() {
        return INSTANCE;
    }

    public void register(Object obj) {
        if (registeredObjects.contains(obj)) {
            return;
        }

        registeredObjects.add(obj);

        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                method.setAccessible(true);
                registerMethodAsListener(obj, method);
            }
        }
    }

    public void unregister(Object obj) {
        if (!registeredObjects.contains(obj)) {
            return;
        }

        registeredObjects.remove(obj);

        preMotionListeners.removeIf(listener -> isFromObject(listener, obj));
        postMotionListeners.removeIf(listener -> isFromObject(listener, obj));
        packetReceiveListeners.removeIf(listener -> isFromObject(listener, obj));
        preVelocityListeners.removeIf(listener -> isFromObject(listener, obj));
        postVelocityListeners.removeIf(listener -> isFromObject(listener, obj));
    }

    private boolean isFromObject(Object listener, Object obj) {
        return listener instanceof MethodListener && ((MethodListener) listener).getOwner() == obj;
    }

    private void registerMethodAsListener(Object obj, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();

        if (paramTypes.length == 0) {
            String methodName = method.getName();
            if (methodName.contains("PreMotion") || methodName.contains("preMotion")) {
                preMotionListeners.add(new MethodListener(obj, method)::invokePreMotion);
            } else if (methodName.contains("PostMotion") || methodName.contains("postMotion")) {
                postMotionListeners.add(new MethodListener(obj, method)::invokePostMotion);
            }
        } else if (paramTypes.length == 1) {
            Class<?> paramType = paramTypes[0];

            if (Packet.class.isAssignableFrom(paramType)) {
                packetReceiveListeners.add(new MethodListener(obj, method)::invokePacketReceive);
            } else if (paramType.getSimpleName().contains("PreVelocity")) {
                preVelocityListeners.add(new MethodListener(obj, method)::invokePreVelocity);
            } else if (paramType.getSimpleName().contains("PostVelocity")) {
                postVelocityListeners.add(new MethodListener(obj, method)::invokePostVelocity);
            }
        }
    }

    public void add(Class<?> type, Object listener) {
        if (type == PreMotionListener.class && listener instanceof PreMotionListener) {
            preMotionListeners.add((PreMotionListener) listener);
        } else if (type == PostMotionListener.class && listener instanceof PostMotionListener) {
            postMotionListeners.add((PostMotionListener) listener);
        } else if (type == PacketReceiveListener.class && listener instanceof PacketReceiveListener) {
            packetReceiveListeners.add((PacketReceiveListener) listener);
        } else if (type == PreVelocityListener.class && listener instanceof PreVelocityListener) {
            preVelocityListeners.add((PreVelocityListener) listener);
        } else if (type == PostVelocityListener.class && listener instanceof PostVelocityListener) {
            postVelocityListeners.add((PostVelocityListener) listener);
        }
    }

    public void remove(Class<?> type, Object listener) {
        if (type == PreMotionListener.class) {
            preMotionListeners.remove(listener);
        } else if (type == PostMotionListener.class) {
            postMotionListeners.remove(listener);
        } else if (type == PacketReceiveListener.class) {
            packetReceiveListeners.remove(listener);
        } else if (type == PreVelocityListener.class) {
            preVelocityListeners.remove(listener);
        } else if (type == PostVelocityListener.class) {
            postVelocityListeners.remove(listener);
        }
    }

    public void firePreMotion() {
        for (PreMotionListener listener : new ArrayList<>(preMotionListeners)) {
            listener.onPreMotion();
        }
    }

    public void firePostMotion() {
        for (PostMotionListener listener : new ArrayList<>(postMotionListeners)) {
            listener.onPostMotion();
        }
    }

    public void firePacketReceive(Packet<?> packet) {
        for (PacketReceiveListener listener : new ArrayList<>(packetReceiveListeners)) {
            listener.onPacketReceive(packet);
        }
    }

    public void firePreVelocity(PreVelocityEvent event) {
        for (PreVelocityListener listener : new ArrayList<>(preVelocityListeners)) {
            listener.onPreVelocity(event);
        }
    }

    public void firePostVelocity(PostVelocityEvent event) {
        for (PostVelocityListener listener : new ArrayList<>(postVelocityListeners)) {
            listener.onPostVelocity(event);
        }
    }

    private static class MethodListener implements PreMotionListener, PostMotionListener, PacketReceiveListener, PreVelocityListener, PostVelocityListener {
        private final Object owner;
        private final Method method;

        public MethodListener(Object owner, Method method) {
            this.owner = owner;
            this.method = method;
        }

        public Object getOwner() {
            return owner;
        }

        public void invokePreMotion() {
            try {
                method.invoke(owner);
            } catch (Exception e) {
                System.err.println("Error invoking PreMotion event handler: " + e.getMessage());
            }
        }

        public void invokePostMotion() {
            try {
                method.invoke(owner);
            } catch (Exception e) {
                System.err.println("Error invoking PostMotion event handler: " + e.getMessage());
            }
        }

        public void invokePacketReceive(Packet<?> packet) {
            try {
                method.invoke(owner, packet);
            } catch (Exception e) {
                System.err.println("Error invoking PacketReceive event handler: " + e.getMessage());
            }
        }

        public void invokePreVelocity(PreVelocityEvent event) {
            try {
                method.invoke(owner, event);
            } catch (Exception e) {
                System.err.println("Error invoking PreVelocity event handler: " + e.getMessage());
            }
        }

        public void invokePostVelocity(PostVelocityEvent event) {
            try {
                method.invoke(owner, event);
            } catch (Exception e) {
                System.err.println("Error invoking PostVelocity event handler: " + e.getMessage());
            }
        }

        @Override
        public void onPreMotion() {
            invokePreMotion();
        }

        @Override
        public void onPostMotion() {
            invokePostMotion();
        }

        @Override
        public void onPacketReceive(Packet<?> packet) {
            invokePacketReceive(packet);
        }

        @Override
        public void onPreVelocity(PreVelocityEvent event) {
            invokePreVelocity(event);
        }

        @Override
        public void onPostVelocity(PostVelocityEvent event) {
            invokePostVelocity(event);
        }
    }
}