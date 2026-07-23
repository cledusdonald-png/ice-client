package com.iceclient.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A pet built from a JSON file, so designing one needs no Java and no rebuild.
 *
 * <p>Pets used to be hard-coded shapes, which meant every idea had to go through
 * a compile. The format below is deliberately blunt -- a list of boxes and
 * spikes with positions, sizes and colours -- because that is enough to make
 * something recognisable at the size a pet actually renders, and because a
 * format you can hold in your head is one you will actually use.
 *
 * <pre>
 * {
 *   "name": "Snow Golem",
 *   "scale": 1.0,
 *   "spin": true,
 *   "bob": true,
 *   "parts": [
 *     { "shape": "box",   "pos": [0, 0,    0], "size": [0.22, 0.22, 0.22], "color": "E8F4FA" },
 *     { "shape": "box",   "pos": [0, 0.24, 0], "size": [0.15, 0.15, 0.15], "color": "FFFFFF" },
 *     { "shape": "spike", "pos": [0, 0.32, 0.08], "size": [0.05, 0.14],    "color": "FF8A3D" }
 *   ]
 * }
 * </pre>
 *
 * <p>Coordinates are in blocks, relative to the pet's own centre: +y is up, +z
 * is forward. A box's {@code size} is width/height/depth; a spike's is
 * base/length and it points up unless {@code "down": true}.
 */
public final class PetModel {

   public final String name;
   public final float scale;
   public final boolean spin;
   public final boolean bob;
   /**
    * Whether this one belongs on a shoulder when nothing says otherwise.
    *
    * <p>Only a default. Something that flies or perches looks wrong walking, and
    * something with four legs looks wrong clinging to a collarbone, but the user
    * decides -- this just picks the sensible starting point.
    */
   public boolean perchByDefault;
   public final List<Part> parts = new ArrayList<Part>();

   /** One shape. Kept as plain fields; this is data, not behaviour. */
   public static final class Part {
      public String shape = "box";
      public float x, y, z;
      public float w = 0.2F, h = 0.2F, d = 0.2F;
      public boolean down;
      public int color = 0xFFFFFF;
      public float alpha = 1.0F;
   }

   /**
    * Builds a model in code, for the pets that ship in the jar.
    *
    * <p>Exists so a shipped pet and a designed one are the same object and share
    * one renderer -- a design worked out in a JSON file becomes a catalogue pet
    * by transcribing the numbers, with no new rendering code.
    */
   public static Builder builder(String name, float scale) {
      return new Builder(name, scale);
   }

   public static final class Builder {
      private final PetModel model;

      private Builder(String name, float scale) {
         // Ground pets do not spin; they walk, and turning them looks broken.
         this.model = new PetModel(name, scale, false, true);
      }

      public Builder box(float x, float y, float z, float w, float h, float d, int color) {
         Part p = new Part();
         p.shape = "box";
         p.x = x;
         p.y = y;
         p.z = z;
         p.w = w;
         p.h = h;
         p.d = d;
         p.color = color;
         this.model.parts.add(p);
         return this;
      }

      public Builder spike(float x, float y, float z, float base, float len, int color, boolean down) {
         Part p = new Part();
         p.shape = "spike";
         p.x = x;
         p.y = y;
         p.z = z;
         p.w = base;
         p.h = len;
         p.d = base;
         p.color = color;
         p.down = down;
         this.model.parts.add(p);
         return this;
      }

      /** Marks this pet as one that belongs on a shoulder unless told otherwise. */
      public Builder perches() {
         this.model.perchByDefault = true;
         return this;
      }

      public PetModel build() {
         return this.model;
      }
   }

   private PetModel(String name, float scale, boolean spin, boolean bob) {
      this.name = name;
      this.scale = scale;
      this.spin = spin;
      this.bob = bob;
   }

   /**
    * Reads one pet file.
    *
    * @return the model, or null if the file is unreadable -- callers report the
    *         filename rather than failing silently, since a typo in a pet you
    *         just wrote should be visible
    */
   public static PetModel load(File file) throws Exception {
      Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
      JsonObject root;

      try {
         root = new JsonParser().parse(r).getAsJsonObject();
      } finally {
         r.close();
      }

      String name = root.has("name")
            ? root.get("name").getAsString()
            : file.getName().replaceAll("\\.json$", "");

      PetModel m = new PetModel(
            name,
            root.has("scale") ? root.get("scale").getAsFloat() : 1.0F,
            !root.has("spin") || root.get("spin").getAsBoolean(),
            !root.has("bob") || root.get("bob").getAsBoolean());

      if(!root.has("parts")) {
         throw new IllegalArgumentException("no \"parts\" list");
      }

      JsonArray parts = root.getAsJsonArray("parts");
      for(JsonElement e : parts) {
         m.parts.add(readPart(e.getAsJsonObject()));
      }

      if(m.parts.isEmpty()) {
         throw new IllegalArgumentException("\"parts\" is empty");
      }

      // A runaway part count is a typo, not a design, and every part is drawn
      // for every pet every frame.
      if(m.parts.size() > 40) {
         throw new IllegalArgumentException("too many parts (" + m.parts.size() + ", max 40)");
      }

      return m;
   }

   private static Part readPart(JsonObject o) {
      Part p = new Part();

      if(o.has("shape")) {
         p.shape = o.get("shape").getAsString().toLowerCase();
      }

      if(o.has("pos")) {
         JsonArray a = o.getAsJsonArray("pos");
         p.x = a.get(0).getAsFloat();
         p.y = a.get(1).getAsFloat();
         p.z = a.get(2).getAsFloat();
      }

      if(o.has("size")) {
         JsonArray a = o.getAsJsonArray("size");
         if(a.size() >= 3) {
            p.w = a.get(0).getAsFloat();
            p.h = a.get(1).getAsFloat();
            p.d = a.get(2).getAsFloat();
         } else if(a.size() == 2) {
            // Spikes take base/length.
            p.w = a.get(0).getAsFloat();
            p.h = a.get(1).getAsFloat();
            p.d = p.w;
         }
      }

      if(o.has("down")) {
         p.down = o.get("down").getAsBoolean();
      }

      if(o.has("color")) {
         String c = o.get("color").getAsString().replace("#", "");
         p.color = (int)Long.parseLong(c, 16) & 0xFFFFFF;
      }

      if(o.has("alpha")) {
         p.alpha = Math.max(0.0F, Math.min(1.0F, o.get("alpha").getAsFloat()));
      }

      return p;
   }
}
