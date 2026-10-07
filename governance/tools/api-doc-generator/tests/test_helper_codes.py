"""Codes raised through shared helpers -- the code is the caller's argument, or the argument of the
constructor that built the helper object, or a constant inside a shared class the caller
instantiates -- are bound to the caller's site with the helper's own Status."""
import unittest

from tests._paths import FIXTURES
from extractors import business_error_extractor as bx
from models.api_doc_model import ApiDocument, Endpoint, ErrorCode

HELPERS = FIXTURES / "helpers"
STATUS_HTTP = {"ALREADY_EXISTS": "CONFLICT", "VALIDATION_ERROR": "BAD_REQUEST"}


def codes(errors):
    return sorted({(e.code, e.status, e.throw_site) for e in errors})


class SharedHelperCodes(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.classes = bx.index_module_source(HELPERS / "module")
        cls.helpers = bx.index_code_helpers([HELPERS / "module", HELPERS / "shared"])

    def walk(self, method):
        return bx.walk_endpoint(self.classes, "HxController", method, helpers=self.helpers)[0]

    def test_helper_index_reads_the_code_parameter_and_the_status_from_source(self):
        self.assertEqual(self.helpers.params[("HxRules", "assertUnique")], [(1, "ALREADY_EXISTS")])
        self.assertEqual(self.helpers.params[("HxRules", "assertNotBlank")], [(0, "VALIDATION_ERROR")])
        self.assertEqual(self.helpers.params[("HxLookups", "read")], [(2, "NOT_FOUND")])
        self.assertEqual(self.helpers.bound["HxTransitions"], (1, {"assertAllowed": "BUSINESS_RULE_VIOLATION"}))
        self.assertEqual(self.helpers.fixed["HxInstantConverter"], [("VALIDATION_ERROR", "VALIDATION_ERROR")])

    def test_static_helper_calls_bind_the_argument_code_at_the_callers_site(self):
        self.assertEqual(codes(self.walk("create")), [
            ("HX_400_NAME_REQUIRED", "VALIDATION_ERROR", "HxDomain.create"),
            ("HX_409_CODE_DUP", "ALREADY_EXISTS", "HxDomain.create"),
        ])

    def test_constructor_bound_helper_field_binds_the_constructor_code(self):
        self.assertEqual(codes(self.walk("move")), [
            ("HX_404_ITEM", "NOT_FOUND", "HxService.move"),
            ("HX_422_BAD_TRANSITION", "BUSINESS_RULE_VIOLATION", "HxDomain.assertCanMoveTo"),
        ])

    def test_lookup_helper_and_instantiated_shared_converter(self):
        self.assertEqual(codes(self.walk("lookup")), [("HX_404_LOOKUP_KEY", "NOT_FOUND", "HxService.lookup")])
        self.assertEqual(codes(self.walk("search")), [("VALIDATION_ERROR", "VALIDATION_ERROR", "HxService.converter")])

    def test_without_helpers_nothing_is_bound_as_before(self):
        self.assertEqual(codes(bx.walk_endpoint(self.classes, "HxController", "create")[0]), [])

    def test_throw_sites_and_registered_status_come_from_helper_call_sites(self):
        sites = bx.count_throw_sites(self.classes, self.helpers)
        self.assertEqual(sites["HX_409_CODE_DUP"], ["HxDomain.create"])
        self.assertEqual(sites["HX_422_BAD_TRANSITION"], ["HxDomain.assertCanMoveTo"])
        document = ApiDocument(module="HX")
        document.error_codes = [ErrorCode(name="HX_409_CODE_DUP", value="HX-409-CODE-DUP", source_file="x")]
        ep = Endpoint(method="POST", path="/api/v1/hx/items")
        document.endpoints = [ep]
        bx.attach_business_errors(document, self.classes, {id(ep): ("HxController", "create")},
                                  STATUS_HTTP, self.helpers)
        code = document.error_codes[0]
        self.assertEqual(code.status, "ALREADY_EXISTS")
        self.assertEqual(code.bound_endpoints, 1)
        self.assertIn(("HX_409_CODE_DUP", "ALREADY_EXISTS", "HxDomain.create"), codes(ep.business_errors))


if __name__ == "__main__":
    unittest.main()
