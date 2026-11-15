# Magic Book (Fabric 1.20.1)

หนังสือเวทมนตร์ 3D ร่ายเวทด้วยเสียงพูดภาษาไทย

## วิธีเล่น
1. คราฟต์: หนังสือ + เศษอเมทิสต์ (หรือ /give @s magicbook:magic_book)
2. คลิกขวาค้าง -> ไมค์เปิด พูดว่า "หนังสือของฉัน" -> สุ่มสี แดง/ฟ้า/เหลือง/เขียว
3. พูดคำร่ายเวท "ครั้งแรก" = ปลดล็อก (ใช้ของในกระเป๋าครั้งเดียว) ครั้งต่อไป = ร่ายจริง

| คำพูด | ผล | ของปลดล็อก |
|---|---|---|
| บอลไฟ | ยิงบอลไฟ ติดไฟ | Blaze Powder x4 |
| บอลน้ำ | ผลักถอย ดับไฟ ช้าลง | Prismarine Crystals x4 |
| บอลดิน | ดาเมจหนัก ผลักแรง ช้า | Iron Ingot x3 |
| บอลแสง | ส่องสว่าง (Glowing) แรงกับอันเดด | Glowstone Dust x8 |
| บอลมืด | ตาบอด + Wither | Ender Pearl x2 |
| จงรักษา | ฮีลตัวเองและผู้เล่นรอบข้าง | Golden Apple x1 |
| มวลสารจงทะลายกฎเกณฑ์... ดูม | หลุมดำ 20 วิ ดูดแล้วพ่นออกพร้อมดาเมจ | Nether Star x1 (คูลดาวน์ 60 วิ) |

ปรับของปลดล็อก/ดาเมจ/คูลดาวน์ได้ใน Spell.java และ BlackHole.java

## ติดตั้งระบบเสียง (ฝั่งผู้เล่น)
ดาวน์โหลด ggml-small.bin (Whisper multilingual) จาก https://huggingface.co/ggerganov/whisper.cpp
แล้ววางที่ `.minecraft/config/magicbook/ggml-small.bin`
(เครื่องช้าใช้ ggml-base.bin เปลี่ยนชื่อไฟล์ใน VoiceListener.init ได้) ต้องกดค้างไว้จนเอฟเฟกต์ออก เพราะถอดเสียงใช้เวลา 1-3 วิ

## ทดสอบโดยไม่ใช้ไมค์
ถือหนังสือในมือหลัก แล้วพิมพ์ `/magicbook say บอลไฟ` (ต้องเปิด cheats)

## Build
ใช้ Gradle wrapper จาก Fabric example mod (หรือ `gradle wrapper`) แล้วรัน `./gradlew build` / `./gradlew runClient`
ไฟล์ jar อยู่ใน build/libs

## Build ง่ายๆ ไม่ต้องลงอะไร (GitHub)
อัปโหลดโฟลเดอร์นี้ขึ้น GitHub repo -> แท็บ Actions -> รอ build เสร็จ -> โหลด artifact "magicbook-jar"
(ต้องใช้ Fabric Loader + Fabric API สำหรับ 1.20.1 และ Java 17)
