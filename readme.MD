# AwsS3 – Batch Delete for Amazon S3

A **Java** command-line utility that deletes files from **Amazon S3** based on a list of URLs, with:

- ✅ Input read from a `.txt` file (one URL per line)
- ✅ Deletes in batches of up to **100 objects**
- ✅ Keys grouped **per bucket** (each request targets a single bucket)
- ✅ `--dry-run` mode that lists what would be deleted without calling S3
- ✅ AWS credentials from **environment variables**
- ✅ Built on **AWS SDK v2**
- ✅ Unit tests (JUnit 5 + Mockito) and CI with GitHub Actions

## ⚠️ Warning and known limitations

This tool performs a **destructive** operation. Try it on a test bucket first.

- There is no prior existence check: S3 treats deleting a non-existent key as a success.
- Supported URL formats: `s3://bucket/key`, path-style (`https://s3.<region>.amazonaws.com/bucket/key`) and virtual-hosted-style (`https://bucket.s3.amazonaws.com/key`).
- The exit code is non-zero when there are failures or invalid URLs.

---

## 📋 Prerequisites

- **Java 17 or higher** (recommended: 17 or 21)
- **Apache Maven 3.8+**
- An AWS account with permission to delete objects in S3

Check your setup:

```bash
java -version
mvn -version
```

## Usage

```bash
# configure your credentials and region
export AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY
export AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY
export AWS_REGION=us-east-1

# build
mvn clean package

# simulate (nothing is deleted)
java -jar target/AwsS3-1.0-SNAPSHOT.jar --dry-run /path/to/s3-files.txt

# run for real
java -jar target/AwsS3-1.0-SNAPSHOT.jar /path/to/s3-files.txt

# tests
mvn test
```
