# AwsS3 – Batch Delete de Arquivos no S3

Este projeto é um utilitário em **Java** para deletar arquivos do **Amazon S3** a partir de uma lista de URLs, com:

- ✅ Leitura de arquivo `.txt`
- ✅ Delete em lotes de até **100 arquivos**
- ✅ Uso de **variáveis de ambiente** para credenciais AWS
- ✅ Compatível com **AWS SDK v2**

## ⚠️ Aviso e limitações conhecidas

Este utilitário executa uma operação **destrutiva**. Teste primeiro em um bucket de teste.

- Não há verificação prévia de existência do objeto: o S3 trata a remoção de uma chave inexistente como sucesso.
- Não há modo `dry-run`.
- O agrupamento por bucket ainda não é confiável quando o mesmo arquivo mistura buckets diferentes. Use um arquivo de entrada por bucket.

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
```

## Execução

```bash
# configurar variáveis de ambiente com suas chaves
export AWS_ACCESS_KEY_ID=SEU_ACCESS_KEY
export AWS_SECRET_ACCESS_KEY=SEU_SECRET_KEY
export AWS_REGION=us-east-1

# build do projeto
mvn clean package

# executar o projeto
java -jar target/AwsS3-1.0-SNAPSHOT.jar /home/usuario/arquivos/s3-files.txt
```
