# AI-NOTES

## AI tools used

I used ChatGPT during the development of this exercise as a technical assistant.

I mainly used it for:

- reviewing the interpretation of the admission rules;
- checking edge cases;
- debugging ranking and invitation behaviour;
- designing automated tests;
- reviewing the frontend requirements;
- improving the README and project documentation.

## How it went

AI was useful for speeding up code review and for identifying cases that were easy to overlook.

The most useful part was using it as a second reviewer against the exercise specification. Instead of only asking for code, I used it to compare the implementation with the written requirements and to look for inconsistencies.

It was also helpful for proposing test cases for business rules such as:

- invitation handling;
- veto priority;
- ranking tie-breakers;
- deterministic results after shuffling the input;
- house assignment.

## Important prompts

Some of the prompts that were useful were similar to:

> Review this Java implementation against the exercise specification and identify any rule that is not implemented correctly.

> Check whether invited applicants are always accepted and whether they still keep their real ranking position.

> Suggest JUnit tests that validate the business rules rather than only testing implementation details.

> Check that changing the order of the applications does not change the final result.

> Review the UI and tell me whether it satisfies the minimum requirements in the exercise.

## Where AI gave wrong or incomplete answers

AI was not always correct on the first attempt.

One important example was the handling of invited applicants.

An early proposed solution guaranteed the invited applicant a place by putting invited applicants before the normal ranking. This caused the invited applicant to receive rank 1 even when their admission score was much lower than other candidates.

After reviewing the exercise again, I corrected this behaviour.

The final implementation keeps two concepts separate:

- the ranking position is calculated from the admission score and tie-break rules;
- the invitation guarantees admission and consumes one of the available places.

This means an invited applicant can have a low ranking position and still be accepted.

This was a useful reminder that AI suggestions still need to be checked carefully against the original requirements.

Another example was the deterministic test. An initial test only processed the same input twice, which did not actually prove that input order was irrelevant. I changed it so that the applications are shuffled before the second execution and the outputs are then compared.

## What I preferred to do myself

I preferred to make the final decisions about:

- the interpretation of the business rules;
- the overall project structure;
- how the Java backend and frontend interact;
- which features were worth adding to the UI;
- the final code changes;
- validating the output against the supplied JSON files.

I also manually tested the web interface with different application JSON files and checked specific cases such as rejected applicants and invited applicants.

## Final thoughts

AI helped me move faster, especially during review and testing, but I treated its output as suggestions rather than as a source of truth.

The exercise specification and the actual program behaviour were always used as the final reference.