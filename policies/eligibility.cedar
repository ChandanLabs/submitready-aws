// Demonstration-only policy for the hackathon MVP.
// This is NOT an official government scholarship eligibility rule.
// In production, scheme-specific rules would be sourced from the authoritative program owner.
permit(
  principal,
  action == Action::"submitApplication",
  resource
)
when {
  resource.domicileState == "Andhra Pradesh"
};
