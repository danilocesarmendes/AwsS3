# AwsS3 – Batch Delete de Arquivos no S3

Este projeto é um utilitário em **Java** para deletar arquivos do **Amazon S3** a partir de uma lista de URLs, com:

- ✅ Leitura de arquivo `.txt`
- ✅ Delete em lotes de até **100 arquivos**
- ✅ Uso de **variáveis de ambiente** para credenciais AWS
- ✅ Compatível com **AWS SDK v2**

## ⚠️ Aviso e limitações conhecidas

Este utilitário executa uma operação **destrutiva**. Teste primeiro em um bucket de teste.

- Não há verificação prévia de existência do objeto: o S3 trata a remoção de uma chave inexistente como sucesso.
- Use `--dry-run` para listar o que seria removido, sem chamar o S3.
- Cada requisição ao S3 contém chaves de um único bucket, então um mesmo arquivo pode misturar buckets.

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

# simular (nada é removido)
java -jar target/AwsS3-1.0-SNAPSHOT.jar --dry-run /home/usuario/arquivos/s3-files.txt

# executar de verdade
java -jar target/AwsS3-1.0-SNAPSHOT.jar /home/usuario/arquivos/s3-files.txt

# testes
mvn test
```
