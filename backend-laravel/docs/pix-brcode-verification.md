# Pix BR Code verification

Date: 2026-05-16

## What changed

- The backend no longer returns an internal Nexora reference as if it were a bank Pix code.
- Support contributions generate Pix copia e cola with `NEXORA_SUPPORT_PIX_KEY`, a platform Pix key owned by Nexora, so the requester's Pix key and name are not embedded in the app-generated payload.
- `NEXORA_ADMIN_PIX_KEY` is used for administrative fees. `NEXORA_SUPPORT_PIX_KEY` is required for normal support transfers and can be an EVP/random Pix key or a PJ CNPJ Pix key.
- The Pix payload now follows BR Code/EMV fields:
  - `00` payload format
  - `01` point of initiation
  - `26` merchant account with `br.gov.bcb.pix`
  - `52` category `0000`
  - `53` currency `986`
  - `54` amount
  - `58` country `BR`
  - `59` merchant name
  - `60` merchant city
  - `62/05` reference label
  - `63` CRC16
- Phone Pix keys are normalized to `+55...` when they are phone numbers. Valid CPF keys remain as 11 digits.
- The super admin CPF can be synchronized from `NEXORA_SUPER_ADMIN_CPF` for CPF login.

## Production verification

Backend URL: https://nexoraappbr.com/api

Verified:

- `GET /health` returns the Laravel backend marker.
- Login by CPF `11976639247` succeeds with the configured account password.
- A fresh support request was created and approved.
- A donor created a contribution and received a Pix copia e cola payload for the platform support Pix key.
- The generated payload:
  - starts with `000201`
  - contains `br.gov.bcb.pix`
  - contains currency `5303986`
  - contains country `5802BR`
  - ends with `6304` plus four hexadecimal CRC characters
  - passes local CRC16 validation

Verification request code: `AP-CBQ88RH`

## Important limitation

The app can validate BR Code structure and CRC. A bank will only locate the Pix if `NEXORA_SUPPORT_PIX_KEY` is actually registered in the Brazilian banking system. Static Pix copia e cola cannot hide the receiver from the bank app, because the bank must resolve a real key or dynamic Pix URL before payment confirmation.

## Status flow

- `Aguardando revisao`: user or request is waiting for admin approval.
- `Aberto`: support request can receive Pix contributions.
- `Aguardando comprovantes`: Pix instruction was generated; sender and receiver still need to upload transaction ID plus receipt image.
- `Completo`: request amount was fully allocated.
- `Validado`: admin reviewed both sides and confirmed the Pix.
- `Retornado`: admin confirmed the later return/repayment step.
- `Recusado`: request was rejected.
- `Bloqueado`: user cannot act until admin changes status.
