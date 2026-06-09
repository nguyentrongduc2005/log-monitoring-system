import { useState, type FormEvent } from "react";
import { isAxiosError } from "axios";
import {
  Navigate,
  useLocation,
  useNavigate,
  type Location
} from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

type LoginLocationState = {
  from?: Location;
};

function getLoginError(error: unknown) {
  if (isAxiosError(error)) {
    if (!error.response) {
      return "Unable to reach LogPulse. Check the server connection.";
    }

    const responseData = error.response.data;
    const message =
      typeof responseData === "object" &&
      responseData !== null &&
      "message" in responseData &&
      typeof responseData.message === "string"
        ? responseData.message
        : null;

    return message || "Authentication failed.";
  }

  return "Authentication failed. Please try again.";
}

export function Component() {
  const { session, isInitializing, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const destination =
    (() => {
      const previousLocation = (location.state as LoginLocationState | null)
        ?.from;

      return previousLocation
        ? `${previousLocation.pathname}${previousLocation.search}${previousLocation.hash}`
        : "/";
    })();

  if (isInitializing) {
    return (
      <main className="flex min-h-svh items-center justify-center bg-[#0b0d0f] text-sm text-[#c2c6d6]">
        Restoring secure session...
      </main>
    );
  }

  if (session) {
    return <Navigate to={destination} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      await login({ email: email.trim(), password });
      navigate(destination, { replace: true });
    } catch (requestError) {
      setError(getLoginError(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="relative isolate flex min-h-svh items-center justify-center overflow-hidden bg-[#0b0d0f] px-4 py-10 text-[#e3e2e3]">
      <div
        className="absolute inset-0 -z-20 opacity-80"
        style={{
          backgroundImage:
            "linear-gradient(rgba(45,49,57,.1) 1px,transparent 1px),linear-gradient(90deg,rgba(45,49,57,.1) 1px,transparent 1px)",
          backgroundSize: "40px 40px"
        }}
      />
      <div className="absolute left-1/2 top-0 -z-10 h-72 w-[min(600px,100vw)] -translate-x-1/2 bg-[#1868db]/10 blur-[120px]" />

      <div className="flex w-full max-w-[400px] flex-col items-center">
        <header className="mb-8 text-center">
          <div
            aria-hidden="true"
            className="mx-auto mb-2 flex h-10 w-10 items-center justify-center border border-[#aec6ff]/40 bg-[#aec6ff]/5 font-mono text-lg font-semibold text-[#aec6ff]"
          >
            &gt;_
          </div>
          <h1 className="text-xl font-bold text-[#aec6ff]">LogPulse</h1>
          <p className="mt-1 text-xs uppercase tracking-[0.16em] text-[#c2c6d6]">
            Observability Platform
          </p>
        </header>

        <section className="w-full rounded bg-[#16181d] p-6 shadow-2xl ring-1 ring-[#2d3139] sm:p-10">
          <header className="mb-6 text-center">
            <h2 className="text-xl font-semibold text-[#e3e2e3]">Sign in</h2>
            <p className="mt-1 text-xs text-[#c2c6d6]">
              Access your telemetry dashboard
            </p>
          </header>

          <form className="space-y-4" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-1">
              <label
                className="text-[10px] font-bold uppercase tracking-[0.05em] text-[#c2c6d6]"
                htmlFor="email"
              >
                Email address
              </label>
              <input
                autoComplete="email"
                autoFocus
                className="w-full rounded border border-[#2d3139] bg-[#0b0d0f] px-4 py-2 text-sm text-[#e3e2e3] outline-none transition placeholder:text-[#646975] focus:border-[#1868db] focus:ring-2 focus:ring-[#1868db]/20"
                id="email"
                name="email"
                onChange={(event) => setEmail(event.target.value)}
                placeholder="engineer@logpulse.io"
                required
                type="email"
                value={email}
              />
            </div>

            <div className="flex flex-col gap-1">
              <div className="flex items-center justify-between gap-4">
                <label
                  className="text-[10px] font-bold uppercase tracking-[0.05em] text-[#c2c6d6]"
                  htmlFor="password"
                >
                  Password
                </label>
                <a
                  className="text-xs text-[#aec6ff] hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#aec6ff]"
                  href="mailto:support@logpulse.io?subject=Password reset"
                >
                  Forgot password?
                </a>
              </div>
              <div className="relative">
                <input
                  autoComplete="current-password"
                  className="w-full rounded border border-[#2d3139] bg-[#0b0d0f] px-4 py-2 pr-16 text-sm text-[#e3e2e3] outline-none transition placeholder:text-[#646975] focus:border-[#1868db] focus:ring-2 focus:ring-[#1868db]/20"
                  id="password"
                  name="password"
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Enter your password"
                  required
                  type={showPassword ? "text" : "password"}
                  value={password}
                />
                <button
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-[#c2c6d6] hover:text-[#e3e2e3] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-3px] focus-visible:outline-[#aec6ff]"
                  onClick={() => setShowPassword((visible) => !visible)}
                  type="button"
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {error && (
              <p
                className="border-l-2 border-[#ffb4ab] bg-[#93000a]/20 px-3 py-2 text-xs text-[#ffdad6]"
                role="alert"
              >
                {error}
              </p>
            )}

            <div className="pt-2">
              <button
                className="flex w-full items-center justify-center rounded bg-[#1868db] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#2075ee] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#aec6ff] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting ? "Authenticating..." : "Authenticate"}
              </button>
            </div>
          </form>

          <footer className="mt-8 border-t border-[#424753] pt-6 text-center">
            <p className="text-xs text-[#c2c6d6]">
              Don&apos;t have an account?{" "}
              <a
                className="font-semibold text-[#aec6ff] hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#aec6ff]"
                href="mailto:support@logpulse.io?subject=LogPulse access request"
              >
                Request access
              </a>
            </p>
          </footer>
        </section>

        <p className="mt-8 font-mono text-[11px] uppercase text-[#646975]">
          Secure encrypted session
        </p>
      </div>
    </main>
  );
}
