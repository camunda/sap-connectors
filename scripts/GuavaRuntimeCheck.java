import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** Test-only agent: check the classes selected by the image's actual launcher. */
public class GuavaRuntimeCheck {
  public static void premain(String ignored) throws Exception {
    // Fixed Guava 33.7.2-jre bytes, not version metadata from a shadowed JAR.
    var expected =
        Map.of(
            "com.google.common.collect.CompactHashMap",
            "3fce7f865516d760ceb0c4fa6a9764d658529c1be297dc3163c67d4eece291ac",
            "com.google.common.collect.CompactHashSet",
            "79f299ad41714bac403d9b1295add6a9f3997f209c166dae8ec359a28a0a70db",
            "com.google.common.collect.MapMakerInternalMap$AbstractSerializationProxy",
            "7759f0f346d9ade80dec268ae80da54b789e7b50348c977703e293e3bf6a9b57");
    for (var entry : expected.entrySet()) {
      var type = Class.forName(entry.getKey(), true, ClassLoader.getSystemClassLoader());
      var origin = type.getProtectionDomain().getCodeSource().getLocation().toString();
      if (!origin.startsWith("file:/opt/app/connector-runtime-application-")
          || !origin.endsWith("-with-dependencies.jar")) {
        throw new IllegalStateException("Unexpected Guava class origin: " + origin);
      }
      try (var bytes = type.getResourceAsStream("/" + entry.getKey().replace('.', '/') + ".class")) {
        if (bytes == null) {
          throw new IllegalStateException("Missing loaded Guava class bytes: " + entry.getKey());
        }
        var hash =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.readAllBytes()));
        if (!hash.equals(entry.getValue())) {
          throw new IllegalStateException(
              "Unverified Guava class: " + entry.getKey() + " origin=" + origin + " sha256=" + hash);
        }
        System.out.println(entry.getKey() + " origin=" + origin + " sha256=" + hash);
      }
    }
    System.out.println("PASS: effective runtime Guava classes match fixed 33.7.2-jre");
  }
}
