package com.iceclient.lunar;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal protobuf wire-format reader.
 *
 * <p>Apollo speaks Protocol Buffers, but pulling in protobuf-java plus the
 * generated {@code apollo-protos} classes would shade a megabyte of runtime and
 * a code-generation step into a 1.8.9 Forge mod for the sake of a handful of
 * messages. The wire format itself is small enough to read directly: a stream
 * of {@code (field_number, wire_type)} tags, each followed by a value whose
 * length the type determines.
 *
 * <p>Deliberately schema-less. Without the {@code .proto} files a field number
 * cannot be turned into a name, so {@link #scan} returns the raw numbered
 * fields and lets the caller decide what they mean. That is also what makes it
 * useful for working out an unknown message from real traffic rather than
 * guessing at its layout.
 */
public final class ProtoReader {

   public static final int WIRE_VARINT = 0;
   public static final int WIRE_FIXED64 = 1;
   public static final int WIRE_LENGTH = 2;
   public static final int WIRE_FIXED32 = 5;

   private ProtoReader() {
   }

   /**
    * Reads a base-128 varint.
    *
    * <p>Capped at ten groups: a 64-bit varint cannot be longer, and without the
    * cap a corrupt or hostile payload of continuation bytes would spin here.
    */
   public static long readVarint(ByteBuf buf) {
      long value = 0L;
      int shift = 0;

      while(shift < 70 && buf.isReadable()) {
         byte b = buf.readByte();
         value |= (long)(b & 127) << shift;
         if((b & 128) == 0) {
            return value;
         }

         shift += 7;
      }

      throw new IllegalStateException("varint too long or truncated");
   }

   /**
    * Reads a little-endian fixed-width field.
    *
    * <p>Protobuf's fixed32/fixed64 are always little-endian regardless of
    * platform. Netty's {@code readLongLE}/{@code readIntLE} would do this in one
    * call but do not exist in the netty 1.8.9 ships, and {@code readLong} would
    * silently read big-endian and return garbage -- so the bytes are assembled
    * explicitly.
    */
   private static long readFixed(ByteBuf buf, int width) {
      long value = 0L;

      for(int i = 0; i < width; ++i) {
         value |= ((long)buf.readByte() & 255L) << i * 8;
      }

      return value;
   }

   /**
    * Every field in a message, keyed by field number.
    *
    * <p>A list per number because protobuf allows a field to repeat -- that is
    * how repeated fields are encoded, and dropping the duplicates would silently
    * lose all but the last element.
    *
    * <p>Values are {@code Long} for varints and fixed32/64, and {@code byte[]}
    * for length-delimited fields, which may in turn be a string, raw bytes or a
    * nested message. The reader cannot tell which without a schema, so the
    * caller decides.
    */
   public static Map<Integer, List<Object>> scan(ByteBuf buf) {
      Map<Integer, List<Object>> fields = new LinkedHashMap<Integer, List<Object>>();

      while(buf.isReadable()) {
         long tag = readVarint(buf);
         int number = (int)(tag >>> 3);
         int wire = (int)(tag & 7L);

         if(number <= 0) {
            throw new IllegalStateException("bad field number " + number);
         }

         Object value;
         switch(wire) {
         case WIRE_VARINT:
            value = Long.valueOf(readVarint(buf));
            break;
         case WIRE_FIXED64:
            if(buf.readableBytes() < 8) {
               throw new IllegalStateException("truncated fixed64");
            }

            value = Long.valueOf(readFixed(buf, 8));
            break;
         case WIRE_LENGTH: {
            int len = (int)readVarint(buf);
            if(len < 0 || len > buf.readableBytes()) {
               throw new IllegalStateException("truncated length-delimited field");
            }

            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            value = bytes;
            break;
         }
         case WIRE_FIXED32:
            if(buf.readableBytes() < 4) {
               throw new IllegalStateException("truncated fixed32");
            }

            value = Long.valueOf(readFixed(buf, 4));
            break;
         default:
            // Groups (3 and 4) are deprecated and Apollo does not use them.
            // Continuing past an unknown wire type would desynchronise the
            // whole stream, so stop here rather than emit nonsense.
            throw new IllegalStateException("unsupported wire type " + wire);
         }

         List<Object> list = fields.get(Integer.valueOf(number));
         if(list == null) {
            list = new ArrayList<Object>(1);
            fields.put(Integer.valueOf(number), list);
         }

         list.add(value);
      }

      return fields;
   }

   /** First value of a field as a UTF-8 string, or null. */
   public static String string(Map<Integer, List<Object>> fields, int number) {
      byte[] b = bytes(fields, number);
      return b == null ? null : new String(b, StandardCharsets.UTF_8);
   }

   public static byte[] bytes(Map<Integer, List<Object>> fields, int number) {
      List<Object> l = fields.get(Integer.valueOf(number));
      if(l == null || l.isEmpty()) {
         return null;
      }

      Object o = l.get(0);
      return o instanceof byte[] ? (byte[])o : null;
   }

   /** First value of a varint/fixed field, or {@code def} when absent. */
   public static long number(Map<Integer, List<Object>> fields, int number, long def) {
      List<Object> l = fields.get(Integer.valueOf(number));
      if(l == null || l.isEmpty()) {
         return def;
      }

      Object o = l.get(0);
      return o instanceof Long ? ((Long)o).longValue() : def;
   }

   /** A fixed64 field reinterpreted as the double protobuf encoded into it. */
   public static double doubleAt(Map<Integer, List<Object>> fields, int number, double def) {
      List<Object> l = fields.get(Integer.valueOf(number));
      if(l == null || l.isEmpty() || !(l.get(0) instanceof Long)) {
         return def;
      }

      return Double.longBitsToDouble(((Long)l.get(0)).longValue());
   }

   /** Short human-readable dump, for the debug setting. */
   public static String describe(Map<Integer, List<Object>> fields) {
      StringBuilder sb = new StringBuilder();

      for(Map.Entry<Integer, List<Object>> e : fields.entrySet()) {
         for(Object v : e.getValue()) {
            if(sb.length() > 0) {
               sb.append(", ");
            }

            sb.append('#').append(e.getKey()).append('=');
            if(v instanceof byte[]) {
               byte[] b = (byte[])v;
               sb.append("bytes[").append(b.length).append(']');

               String s = new String(b, StandardCharsets.UTF_8);
               if(isPrintable(s)) {
                  sb.append('"').append(s).append('"');
               }
            } else {
               sb.append(v);
            }
         }
      }

      return sb.toString();
   }

   /** Whether a length-delimited field is worth showing as text in the dump. */
   private static boolean isPrintable(String s) {
      if(s.isEmpty() || s.length() > 120) {
         return false;
      }

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if(c < 32 && c != '\n' && c != '\t') {
            return false;
         }
      }

      return true;
   }
}
