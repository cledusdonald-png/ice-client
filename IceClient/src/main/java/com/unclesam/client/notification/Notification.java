package com.unclesam.client.notification;

/**
 * One toast in the notification queue.
 *
 * <p>Holds its own birth timestamp rather than a countdown so the manager can
 * stay stateless about timing -- it just asks each notification how far through
 * its life it is, which also makes the slide-in/slide-out easing a pure
 * function of the clock instead of something that drifts with frame rate.
 */
public class Notification {

   /** How long the slide takes at each end, in milliseconds. */
   private static final long SLIDE_MS = 250L;

   private final String title;
   private final String message;
   private final Type type;
   private final long bornAt;
   private final long lifetimeMs;

   public Notification(String title, String message, Type type, long lifetimeMs) {
      this.title = title;
      this.message = message;
      this.type = type;
      this.lifetimeMs = Math.max(SLIDE_MS * 2L, lifetimeMs);
      this.bornAt = System.currentTimeMillis();
   }

   public String getTitle() {
      return this.title;
   }

   public String getMessage() {
      return this.message;
   }

   public Type getType() {
      return this.type;
   }

   public long age() {
      return System.currentTimeMillis() - this.bornAt;
   }

   public boolean isExpired() {
      return this.age() >= this.lifetimeMs;
   }

   /**
    * 0 fully off-screen, 1 fully shown. Eases in over the first {@link #SLIDE_MS}
    * and back out over the last, sitting at 1 in between.
    */
   public float visibility() {
      long age = this.age();
      if(age < SLIDE_MS) {
         return ease((float)age / (float)SLIDE_MS);
      }

      long remaining = this.lifetimeMs - age;
      if(remaining < SLIDE_MS) {
         return ease(Math.max(0.0F, (float)remaining / (float)SLIDE_MS));
      }

      return 1.0F;
   }

   /** Cubic ease-out -- fast off the mark, settles gently. */
   private static float ease(float t) {
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   /** Colour of the accent bar down the left edge of the toast. */
   public int accent() {
      switch(this.type) {
      case SUCCESS:
         return 0xFF55FF55;
      case WARNING:
         return 0xFFFFAA00;
      case ERROR:
         return 0xFFFF5555;
      case INFO:
      default:
         return 0xFF8C5AFF;
      }
   }

   public static enum Type {
      INFO,
      SUCCESS,
      WARNING,
      ERROR;
   }
}
