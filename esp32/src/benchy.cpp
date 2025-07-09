#include "benchy.h"

// Define global benchy instance
Benchy_t benchy = {0};  // Initialize to 0s

void printBenchy(const Benchy_t &b, const char *prefix ) {
    if (prefix) {
        Serial.print(prefix);
    } else {
        Serial.println("=== Benchy_t ===");
    }
  

  Serial.printf("Mode: %u\n", b.config.mode);
  Serial.print("MAC Address: ");
  for (int i = 0; i < 6; ++i) {
    Serial.printf("%02X", b.config.mac_address[i]);
    if (i < 5) Serial.print(":");
  }
  Serial.println();

  Serial.printf("Switch Value: 0x%02X\n", b.op.switch_value);

  Serial.print("Servo Values: ");
  for (int i = 0; i < 4; ++i) {
    Serial.printf("%02X ", b.op.servo_values[i]);
  }
  Serial.println();
}

void BenchyCallbacks::onRead(BLECharacteristic *pCharacteristic) {
  Serial.println("[Benchy] Characteristic read");
  pCharacteristic->setValue((uint8_t *)&benchy, sizeof(Benchy_t));
}

void BenchyCallbacks::onWrite(BLECharacteristic *pCharacteristic) {
  std::string value = pCharacteristic->getValue();

  if (value.length() == sizeof(Benchy_t)) {
    memcpy(&benchy, value.data(), sizeof(Benchy_t));
   printBenchy(benchy, "write Benchy complete");
  } else {
    Serial.printf("[Benchy] Unexpected payload length: %d\n", value.length());
  }
}

void BenchyCallbacks::onNotify(BLECharacteristic *pCharacteristic) {
  Serial.println("[Benchy] Notification sent");
}

void BenchyCallbacks::onStatus(BLECharacteristic *pCharacteristic, Status s, uint32_t code) {
  Serial.printf("[Benchy] Status changed: %d, code: %u\n", s, code);
}
