package services

import play.api.Configuration

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import javax.crypto.{Cipher, SecretKeyFactory}
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import scala.concurrent.ExecutionContext
import scala.util.Try

case class SecurityInfo(key: String, salt: String, iv: String)

class Security (
  val configuration: Configuration,
  implicit val ec: ExecutionContext
) extends Service {

  val secretKey: String = configuration.get[String]("enc.secret.key")
  val secretKeyCharArray: Array[Char] = secretKey.toCharArray
  val salt: String = configuration.get[String]("enc.salt")
  val iv: String = configuration.get[String]("enc.iv")

  private val ivParameterSpec = new IvParameterSpec(Base64.getDecoder.decode(iv))
  private val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

  def encrypt(strToEncrypt: String): Try[String] = {
    Try {
      val spec = new PBEKeySpec(secretKeyCharArray, Base64.getDecoder.decode(salt), 10000, 256)
      val tmp = factory.generateSecret(spec)
      val secretKey = new SecretKeySpec(tmp.getEncoded, "AES")

      val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
      Base64.getEncoder.encodeToString(cipher.doFinal(strToEncrypt.getBytes(UTF_8)))
    }
  }

  def decrypt(strToDecrypt: String): Try[String] = {
    Try {
      val spec = new PBEKeySpec(secretKeyCharArray, Base64.getDecoder.decode(salt), 10000, 256)
      val tmp = factory.generateSecret(spec)
      val secretKey =  new SecretKeySpec(tmp.getEncoded, "AES")

      val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
      new String(cipher.doFinal(Base64.getDecoder.decode(strToDecrypt)))
    }
  }
}
