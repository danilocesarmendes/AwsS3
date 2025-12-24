# AwsS3 – Batch Delete de Arquivos no S3

Este projeto é um utilitário em **Java** para deletar arquivos do **Amazon S3** a partir de uma lista de URLs, com:

- ✅ Leitura de arquivo `.txt`
- ✅ Validação se o objeto existe antes de deletar
- ✅ Delete em lotes de até **100 arquivos**
- ✅ Uso de **variáveis de ambiente** para credenciais AWS
- ✅ Compatível com **AWS SDK v2**

---

## 📋 Pré-requisitos

Antes de rodar o projeto, você precisa ter instalado:

- **Java 17 ou superior** (recomendado: 17 ou 21)
- **Apache Maven 3.8+**
- Conta AWS com permissão para deletar objetos no S3

Verifique:

```bash
java -version
mvn -version

# configurar variaveis de ambiente com sua chaves
export AWS_ACCESS_KEY_ID=SEU_ACCESS_KEY
export AWS_SECRET_ACCESS_KEY=SEU_SECRET_KEY
export AWS_REGION=us-east-1

-- build do projeto
mvn clean package

-- Executar o projeto
java -jar target/AwsS3-1.0-SNAPSHOT.jar /home/usuario/arquivos/s3-files.txt
