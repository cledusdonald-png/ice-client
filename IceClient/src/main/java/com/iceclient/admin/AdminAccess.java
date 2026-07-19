package com.iceclient.admin;

/**
 * Unlock gate for the Admin section.
 *
 * <p><b>This is concealment, not security.</b> The passphrase is a string
 * constant in the jar and the unlocked flag is in memory, so anyone who
 * decompiles the client or attaches a debugger walks straight past it. It hides
 * the section from someone glancing at the screen -- a screenshot, a stream, a
 * friend on your PC -- and nothing more. Do not treat it as protection against
 * anything that runs off this machine.
 *
 * <p>Deliberately not persisted: the lock re-arms every launch, because a flag
 * saved to config would both survive sharing the config file and make the gate
 * pointless on a shared machine.
 */
public final class AdminAccess {

   private static final String PASSPHRASE = "iceylover42";

   private static boolean unlocked;

   private AdminAccess() {
   }

   public static boolean isUnlocked() {
      return unlocked;
   }

   /**
    * Checks an attempt and unlocks on success.
    *
    * @return true when the passphrase matched
    */
   public static boolean tryUnlock(String attempt) {
      if(attempt != null && PASSPHRASE.equals(attempt.trim())) {
         unlocked = true;
      }

      return unlocked;
   }

   /** Re-locks, so the section can be hidden again without restarting. */
   public static void lock() {
      unlocked = false;
   }
}
