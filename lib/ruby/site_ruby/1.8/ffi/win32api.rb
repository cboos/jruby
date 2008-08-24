require 'ffi'

module Win32
  CONVENTION = JRuby::FFI::Platform::IS_WINDOWS ? :stdcall : :default
  TypeDefs = {
    'P' => :pointer,
    'I' => :int
  }
  def self.find_type(name)
    code = TypeDefs[name]
    raise TypeError, "Unable to resolve type '#{name}'" unless code
    return code
  end
  def self.map_types(spec)
    types = []
    
    for i in 0..(spec.length - 1)
      types[i] = self.find_type(spec.slice(i,1))
    end
    types
  end
  class API
    def initialize(func, params, ret, lib)
      @invoker = JRuby::FFI.create_invoker(lib, func.to_s, Win32::map_types(params),
        Win32::map_types(ret)[0], CONVENTION)
    end
    def call(*args)
      @invoker.invoke(args)
    end
  end
end