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
      <main className="flex min-h-svh items-center justify-center bg-background text-sm text-muted">
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
    <main className="relative isolate flex min-h-svh items-center justify-center overflow-hidden bg-background px-4 py-10 text-text">
      <div className="flex w-full max-w-[400px] flex-col items-center">
        <header className="mb-8 text-center">
          <div
            aria-hidden="true"
            className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-md border border-primary/30 bg-primary/10 font-mono text-lg font-semibold text-primary-hover"
          >
            &gt;_
          </div>
          <h1 className="text-xl font-bold text-text">LogPulse</h1>
          <p className="mt-1 text-xs uppercase tracking-[0.16em] text-muted">
            Observability Platform
          </p>
        </header>

        <section className="w-full rounded-lg border border-border bg-card p-6 sm:p-10">
          <header className="mb-6 text-center">
            <h2 className="text-xl font-semibold text-text">Sign in</h2>
            <p className="mt-1 text-xs text-muted">
              Access your telemetry dashboard
            </p>
          </header>

          <form className="space-y-4" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-1">
              <label
                className="text-[10px] font-bold uppercase tracking-[0.05em] text-muted"
                htmlFor="email"
              >
                Email address
              </label>
              <input
                autoComplete="email"
                autoFocus
                className="w-full rounded-md border border-border bg-background px-4 py-2 text-sm text-text outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
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
                  className="text-[10px] font-bold uppercase tracking-[0.05em] text-muted"
                  htmlFor="password"
                >
                  Password
                </label>
                <a
                  className="text-xs text-primary-hover hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                  href="mailto:support@logpulse.io?subject=Password reset"
                >
                  Forgot password?
                </a>
              </div>
              <div className="relative">
                <input
                  autoComplete="current-password"
                  className="w-full rounded-md border border-border bg-background px-4 py-2 pr-16 text-sm text-text outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
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
                  className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-muted hover:text-text focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-3px] focus-visible:outline-primary"
                  onClick={() => setShowPassword((visible) => !visible)}
                  type="button"
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {error && (
              <p
                className="rounded-md border border-error/30 bg-error/10 px-3 py-2 text-xs text-error"
                role="alert"
              >
                {error}
              </p>
            )}

            <div className="pt-2">
              <button
                className="flex w-full items-center justify-center rounded-md bg-primary px-4 py-3 text-sm font-bold text-primary-foreground transition hover:bg-primary-hover focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting ? "Authenticating..." : "Authenticate"}
              </button>
            </div>
          </form>

          <footer className="mt-8 border-t border-border pt-6 text-center">
            <p className="text-xs text-muted">
              Don&apos;t have an account?{" "}
              <a
                className="font-semibold text-primary-hover hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                href="mailto:support@logpulse.io?subject=LogPulse access request"
              >
                Request access
              </a>
            </p>
          </footer>
        </section>

        <p className="mt-8 font-mono text-[11px] uppercase text-muted">
          Secure encrypted session
        </p>
      </div>
    </main>
  );
}
