import { useState, type ReactNode } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  PageHeader,
  PageHeaderProvider,
  usePageHeaderSlots
} from "@/shared/layouts/page-header-context";

function SlotHosts({ children }: { children?: ReactNode }) {
  const { setActionsTarget, setTitleTarget } = usePageHeaderSlots();

  return (
    <>
      <div data-testid="title-slot" ref={setTitleTarget} />
      <div data-testid="actions-slot" ref={setActionsTarget} />
      {children}
    </>
  );
}

function Harness() {
  const [visible, setVisible] = useState(true);

  return (
    <PageHeaderProvider>
      <SlotHosts>
        {visible && (
          <PageHeader
            actions={<button type="button">Refresh</button>}
            title="Overview"
          />
        )}
        <button onClick={() => setVisible(false)} type="button">
          Unmount page header
        </button>
      </SlotHosts>
    </PageHeaderProvider>
  );
}

describe("page header slots", () => {
  it("portals title and actions into their hosts", () => {
    render(<Harness />);

    expect(screen.getByTestId("title-slot")).toHaveTextContent("Overview");
    expect(screen.getByTestId("actions-slot")).toHaveTextContent("Refresh");
  });

  it("removes portal content when the page header unmounts", () => {
    render(<Harness />);

    fireEvent.click(screen.getByRole("button", { name: "Unmount page header" }));

    expect(screen.getByTestId("title-slot")).toBeEmptyDOMElement();
    expect(screen.getByTestId("actions-slot")).toBeEmptyDOMElement();
  });

  it("renders empty hosts when no page header is supplied", () => {
    render(
      <PageHeaderProvider>
        <SlotHosts />
      </PageHeaderProvider>
    );

    expect(screen.getByTestId("title-slot")).toBeEmptyDOMElement();
    expect(screen.getByTestId("actions-slot")).toBeEmptyDOMElement();
  });

  it("throws outside the provider", () => {
    expect(() => render(<PageHeader title="Overview" />)).toThrow(
      "Page header components must be used within PageHeaderProvider"
    );
  });
});
