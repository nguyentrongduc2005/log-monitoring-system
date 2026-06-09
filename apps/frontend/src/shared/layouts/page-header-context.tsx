import {
  createContext,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
  type ReactNode
} from "react";
import { createPortal } from "react-dom";

export type PageHeaderSlots = {
  titleTarget: HTMLElement | null;
  actionsTarget: HTMLElement | null;
  setTitleTarget: (element: HTMLElement | null) => void;
  setActionsTarget: (element: HTMLElement | null) => void;
};

const PageHeaderContext = createContext<PageHeaderSlots | null>(null);

export function PageHeaderProvider({ children }: PropsWithChildren) {
  const [titleTarget, setTitleTarget] = useState<HTMLElement | null>(null);
  const [actionsTarget, setActionsTarget] = useState<HTMLElement | null>(null);
  const value = useMemo(
    () => ({
      titleTarget,
      actionsTarget,
      setTitleTarget,
      setActionsTarget
    }),
    [actionsTarget, titleTarget]
  );

  return (
    <PageHeaderContext.Provider value={value}>
      {children}
    </PageHeaderContext.Provider>
  );
}

// The hook and provider intentionally share this small layout contract.
// eslint-disable-next-line react-refresh/only-export-components
export function usePageHeaderSlots() {
  const context = useContext(PageHeaderContext);

  if (!context) {
    throw new Error(
      "Page header components must be used within PageHeaderProvider"
    );
  }

  return context;
}

export function PageHeader({
  title,
  actions
}: {
  title: string;
  actions?: ReactNode;
}) {
  const { actionsTarget, titleTarget } = usePageHeaderSlots();

  return (
    <>
      {titleTarget
        ? createPortal(
            <span className="block truncate" title={title}>
              {title}
            </span>,
            titleTarget
          )
        : null}
      {actionsTarget && actions ? createPortal(actions, actionsTarget) : null}
    </>
  );
}
