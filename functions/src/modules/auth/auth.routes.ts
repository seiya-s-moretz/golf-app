import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { requestPhoneOtp, verifyPhoneOtp } from "./auth.service";
import { requestOtpSchema, verifyOtpSchema } from "./auth.validation";

export const authRoutes = Router();

// POST /auth/phone/otp（技術設計書6-1章。認証不要）
authRoutes.post(
  "/phone/otp",
  asyncHandler(async (req, res) => {
    const body = requestOtpSchema.parse(req.body);
    await requestPhoneOtp(body.phone_number);
    res.status(204).send();
  })
);

// POST /auth/phone/verify（技術設計書6-1章、ADR-0006。認証不要）
authRoutes.post(
  "/phone/verify",
  asyncHandler(async (req, res) => {
    const body = verifyOtpSchema.parse(req.body);
    const result = await verifyPhoneOtp(body.phone_number, body.otp_code);
    res.json(result);
  })
);

// POST /users（新規登録）はusersモジュール（/users配下）で扱う。auth.serviceのregisterUser()を呼び出す。
