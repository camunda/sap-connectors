import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** Test-only agent: check the classes selected by the image's actual launcher. */
public class GuavaRuntimeCheck {
  public static void premain(String ignored) throws Exception {
    // Fixed Guava 33.8.0-jre bytes (shipped by camunda/connectors 8.8.26; CVE-2026-102554 was fixed in 33.7.2-jre),
    // not version metadata from a shadowed JAR.
    var expected =
        Map.of(
            "com.google.common.collect.CompactHashMap",
            "bde8d753956361fc22a4c6c0da8d0afa59da2a40fec1fde87069b9f4b6fb9b78",
            "com.google.common.collect.CompactHashSet",
            "f0495c31b7280b3f293006e3d05a51dfbf5d57e5b4d4ffcb2a25dc3234da1850",
            "com.google.common.collect.MapMakerInternalMap$AbstractSerializationProxy",
            "849bf8c66bedaaf22d1c2fd9a786b764bd76587b4c5d112c1b058549aab4784a");
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
    System.out.println("PASS: effective runtime Guava classes match fixed 33.8.0-jre");
  }
}
