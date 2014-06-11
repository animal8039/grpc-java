package com.google.net.stubby;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.InputStream;

import javax.annotation.Nullable;

/**
 * Low-level methods for communicating with a remote server during a single RPC. Unlike normal RPCs,
 * calls may stream any number of requests and responses, although a single request and single
 * response is most common. This API is generally intended for use by stubs, but advanced
 * applications may have need for it.
 *
 * <p>{@link #start} is required to be the first of any methods called.
 *
 * <p>No generic method for determining message receipt or providing acknowlegement is provided.
 * Applications are expected to utilize normal payload messages for such signals, as a response
 * natually acknowledges its request.
 *
 * <p>Methods are guaranteed to be non-blocking. Implementations are not required to be thread-safe.
 */
public abstract class Call<RequestT, ResponseT> {
  /**
   * Callbacks for consuming incoming RPC messages.
   *
   * <p>Implementations are free to block for extended periods of time. Implementations are not
   * required to be thread-safe.
   */
  public abstract static class Listener<T> {
    /**
     * A response context has been received. Any context messages will precede payload messages.
     */
    public abstract ListenableFuture<Void> onContext(String name, InputStream value);

    /**
     * A response payload has been received. For streaming calls, there may be zero payload
     * messages.
     */
    public abstract ListenableFuture<Void> onPayload(T payload);

    /**
     * The Call has been closed. No further sending or receiving will occur. If {@code status} is
     * not equal to {@link Status#OK}, then the call failed.
     */
    public abstract void onClose(Status status);
  }

  /**
   * Start a call, using {@code responseListener} for processing response messages.
   *
   * @param responseListener receives response messages
   * @throws IllegalStateException if call is already started
   */
  public abstract void start(Listener<ResponseT> responseListener);

  /**
   * Prevent any further processing for this Call. No further messages may be sent or will be
   * received. The server is informed of cancellations, but may not stop processing the call.
   * Cancellation is permitted even if previously {@code cancel()}ed or {@link #halfClose}d.
   */
  public abstract void cancel();

  /**
   * Close call for message sending. Incoming messages are unaffected.
   *
   * @throws IllegalStateException if call is already {@code halfClose()}d or {@link #cancel}ed
   */
  public abstract void halfClose();

  /**
   * Send a context message. Context messages are intended for side-channel information like
   * statistics and authentication.
   *
   * @param name key identifier of context
   * @param value context value bytes
   * @throws IllegalStateException if call is {@link #halfClose}d or {@link #cancel}ed
   */
  public void sendContext(String name, InputStream value) {
    sendContext(name, value, null);
  }

  /**
   * Send a context message. Context messages are intended for side-channel information like
   * statistics and authentication.
   *
   * <p>If {@code accepted} is non-{@code null}, then the future will be completed when the flow
   * control window is able to fully permit the context message. If the Call fails before being
   * accepted, then the future will be cancelled. Callers concerned with unbounded buffering should
   * wait until the future completes before sending more messages.
   *
   * @param name key identifier of context
   * @param value context value bytes
   * @param accepted notification for adhering to flow control, or {@code null}
   * @throws IllegalStateException if call is {@link #halfClose}d or {@link #cancel}ed
   */
  public abstract void sendContext(String name, InputStream value,
      @Nullable SettableFuture<Void> accepted);

  /**
   * Send a payload message. Payload messages are the primary form of communication associated with
   * RPCs. Multiple payload messages may exist for streaming calls.
   *
   * @param payload message
   * @throws IllegalStateException if call is {@link #halfClose}d or {@link #cancel}ed
   */
  public void sendPayload(RequestT payload) {
    sendPayload(payload, null);
  }

  /**
   * Send a payload message. Payload messages are the primary form of communication associated with
   * RPCs. Multiple payload messages may be sent for streaming calls.
   *
   * <p>If {@code accepted} is non-{@code null}, then the future will be completed when the flow
   * control window is able to fully permit the payload message. If the Call fails before being
   * accepted, then the future will be cancelled. Callers concerned with unbounded buffering should
   * wait until the future completes before sending more messages.
   *
   * @param payload message
   * @param accepted notification for adhering to flow control, or {@code null}
   * @throws IllegalStateException if call is {@link #halfClose}d or {@link #cancel}ed
   */
  public abstract void sendPayload(RequestT payload, @Nullable SettableFuture<Void> accepted);
}