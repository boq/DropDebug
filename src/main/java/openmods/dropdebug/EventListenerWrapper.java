package openmods.dropdebug;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.IEventListener;

public class EventListenerWrapper implements IEventListener {

    public interface Listener {
        public void onEvent(Event event);
    }

    private final IEventListener wrapped;

    private Listener pre;

    private Listener post;

    public EventListenerWrapper(IEventListener wrapped) {
        this.wrapped = wrapped;
    }

    public void setPre(Listener pre) {
        this.pre = pre;
    }

    public void setPost(Listener post) {
        this.post = post;
    }

    public IEventListener unwrap() {
        return wrapped;
    }

    @Override
    public void invoke(Event event) {
        if (pre != null)
            pre.onEvent(event);
        wrapped.invoke(event);
        if (post != null)
            post.onEvent(event);
    }

}
