# mTLS

This example demonstrates how to use mTLS.

## Generate CA

```bash
mkdir -p examples/mtls/src/main/resources/bundle1
cd examples/mtls/src/main/resources/bundle1
openssl genrsa -out private.key 2048
openssl req -new -key private.key -out request.csr
openssl req -x509 -key private.key -in request.csr -out certificate.crt -days 36500
openssl pkcs12 -export -in certificate.crt -inkey private.key -out springboot.p12 -name springboot -CAfile certificate.crt -caname root
```