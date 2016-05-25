package openmods.dropdebug;

import java.lang.reflect.Constructor;

import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import openmods.dropdebug.EventListenerWrapper.Listener;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.*;

@Mod(modid = DropDebug.MODID, name = "DropDebug", version = "$VERSION$")
public class DropDebug {

    static final String MODID = "dropdebug";

    @EventHandler
    public void severStart(FMLServerStartingEvent evt) {
        evt.registerServerCommand(new CommandControl());
    }

    @Mod.Instance(MODID)
    public static DropDebug instance;

    private boolean isInstalled;

    public boolean isInstalled() {
        return isInstalled;
    }

    private PlayerDropsEvent blankEvent;

    private PlayerDropsEvent getBlankEvent() {
        if (blankEvent == null) {
            try {
                Constructor<PlayerDropsEvent> ctr = PlayerDropsEvent.class.getConstructor();
                ctr.setAccessible(true);
                blankEvent = ctr.newInstance();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return blankEvent;
    }

    public void install() {
        Preconditions.checkState(!isInstalled, "Already installed");

        final EventStateTracker tracker = new EventStateTracker();
        visitListeners(getBlankEvent(), new EventListenerVisitor() {

            @Override
            public IEventListener visitMiddleListener(final int busId, final IEventListener listener, boolean isFirst, boolean isLast) {
                final EventListenerWrapper wrapper = new EventListenerWrapper(listener);

                if (listener instanceof EventPriority) {
                    class PreListener implements Listener {
                        @Override
                        public void onEvent(Event event) {
                            tracker.setEventPhase(busId, (EventPriority)listener, (PlayerDropsEvent)event);
                        }
                    }
                    if (isFirst)
                        wrapper.setPre(new PreListener() {
                            @Override
                            public void onEvent(Event event) {
                                tracker.startTracking(busId, (PlayerDropsEvent)event);
                                super.onEvent(event);
                            }

                        });
                    else
                        wrapper.setPre(new PreListener());

                    if (isLast)
                        wrapper.setPost(new Listener() {
                            @Override
                            public void onEvent(Event event) {
                                tracker.stopTracking(busId, (PlayerDropsEvent)event);
                            }
                        });
                } else {
                    if (isFirst)
                        wrapper.setPre(new Listener() {
                            @Override
                            public void onEvent(Event event) {
                                tracker.startTracking(busId, (PlayerDropsEvent)event);
                            }
                        });

                    class PostListener implements Listener {
                        @Override
                        public void onEvent(Event event) {
                            tracker.trackEvent(busId, listener, (PlayerDropsEvent)event);
                        }
                    }

                    if (isLast)
                        wrapper.setPost(new PostListener() {
                            @Override
                            public void onEvent(Event event) {
                                super.onEvent(event);
                                tracker.stopTracking(busId, (PlayerDropsEvent)event);
                            }
                        });
                    else
                        wrapper.setPost(new PostListener());
                }

                return wrapper;
            }

            @Override
            public void startBus(int busId) {
                Log.info("Starting installing on bus %d", busId);
            }

            @Override
            public void endBus(int busId) {
                Log.info("Finished installing on bus %d", busId);
            }
        });

        isInstalled = true;
    }

    public void uninstall() {
        Preconditions.checkState(isInstalled, "Not installed");

        visitListeners(getBlankEvent(), new EventListenerVisitor() {

            @Override
            public void startBus(int busId) {
                Log.info("Starting uninstalling on bus %d", busId);
            }

            @Override
            public IEventListener visitMiddleListener(int busId, IEventListener listener, boolean isFirst, boolean isLast) {
                return (listener instanceof EventListenerWrapper) ? ((EventListenerWrapper)listener).unwrap() : listener;
            }

            @Override
            public void endBus(int busId) {
                Log.info("Finished uninstalling on bus %d", busId);
            }

        });

        isInstalled = false;
    }

    private interface EventListenerVisitor {
        public void startBus(int busId);

        public IEventListener visitMiddleListener(int busId, IEventListener listener, boolean isFirst, boolean isLast);

        public void endBus(int busId);
    }

    private static void visitListeners(Event event, EventListenerVisitor visitor) {
        final ListenerList allListeners = event.getListenerList();
        try {
            int busId = 0;
            while (true) {
                final IEventListener[] busListeners = allListeners.getListeners(busId);
                visitor.startBus(busId);
                for (int i = 0; i < busListeners.length; i++) {
                    final IEventListener newListener = visitor.visitMiddleListener(busId, busListeners[i], i == 0, i == busListeners.length - 1);
                    busListeners[i] = newListener;
                }
                visitor.endBus(busId);
                busId++;
            }
        } catch (ArrayIndexOutOfBoundsException terribleLoopExitCondition) {}
    }
}
