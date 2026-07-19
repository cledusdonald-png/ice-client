package com.iceclient.ping;

import net.minecraft.util.BlockPos;

/** One placed marker: a block, or a whole chunk column. */
public class Ping {

   private BlockPos pos;
   private final Type type;
   private final long created;

   private int color;
   private long lifetimeMs;

   public Ping(BlockPos pos, Type type, int color, long lifetimeMs) {
      this.pos = pos;
      this.type = type;
      this.color = color;
      this.lifetimeMs = lifetimeMs;
      this.created = System.currentTimeMillis();
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public void setPos(BlockPos pos) {
      this.pos = pos;
   }

   public Type getType() {
      return this.type;
   }

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public long getLifetimeMs() {
      return this.lifetimeMs;
   }

   public void extend(long extraMs) {
      this.lifetimeMs += extraMs;
   }

   public long age() {
      return System.currentTimeMillis() - this.created;
   }

   /** A lifetime of 0 or less means the ping never expires on its own. */
   public boolean isExpired() {
      return this.lifetimeMs > 0L && this.age() >= this.lifetimeMs;
   }

   /** Fades over the last second so a ping does not simply blink out. */
   public float alpha() {
      if(this.lifetimeMs <= 0L) {
         return 1.0F;
      }

      long remaining = this.lifetimeMs - this.age();
      return remaining >= 1000L ? 1.0F : Math.max(0.0F, (float)remaining / 1000.0F);
   }

   public static enum Type {
      BLOCK,
      CHUNK;
   }
}
