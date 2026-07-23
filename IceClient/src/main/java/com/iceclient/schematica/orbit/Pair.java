package com.iceclient.schematica.orbit;

/**
 * The two-value holder {@link OrbitPrinter} is built around.
 *
 * <p>Written to match the shape the ported printer expects rather than to be a
 * general utility -- it nests three deep in the print queue's type, and changing
 * that would mean rewriting the queue.
 */
public class Pair<A, B> {

   private final A first;
   private final B second;

   public Pair(A first, B second) {
      this.first = first;
      this.second = second;
   }

   public static <A, B> Pair<A, B> of(A a, B b) {
      return new Pair<A, B>(a, b);
   }

   public A getFirst() {
      return this.first;
   }

   public B getSecond() {
      return this.second;
   }

   /**
    * A pair that compares the same whichever order its values are in.
    *
    * <p>Used for the "these two blocks count as the same" table: a double slab
    * and a slab are interchangeable in either direction, and listing both
    * orderings by hand would be a standing invitation to miss one.
    */
   public static final class EitherPair<A, B> {

      private final A a;
      private final B b;

      private EitherPair(A a, B b) {
         this.a = a;
         this.b = b;
      }

      public static <A, B> EitherPair<A, B> of(A a, B b) {
         return new EitherPair<A, B>(a, b);
      }

      @Override
      public boolean equals(Object o) {
         if(this == o) {
            return true;
         }

         if(!(o instanceof EitherPair)) {
            return false;
         }

         EitherPair<?, ?> other = (EitherPair<?, ?>)o;
         return (eq(this.a, other.a) && eq(this.b, other.b))
               || (eq(this.a, other.b) && eq(this.b, other.a));
      }

      @Override
      public int hashCode() {
         // Order-independent, or equal pairs could land in different buckets
         // and the set would never match them.
         return (this.a == null ? 0 : this.a.hashCode())
               ^ (this.b == null ? 0 : this.b.hashCode());
      }

      private static boolean eq(Object x, Object y) {
         return x == null ? y == null : x.equals(y);
      }
   }
}
